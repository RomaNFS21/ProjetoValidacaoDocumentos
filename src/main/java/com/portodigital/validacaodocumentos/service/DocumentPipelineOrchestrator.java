package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.CriteriosValidacao;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.ValidacaoResponseDTO;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;
import com.portodigital.validacaodocumentos.domain.entity.DadosExtraidos;
import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.entity.Validacao;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import com.portodigital.validacaodocumentos.exception.DocumentNotFoundException;
import com.portodigital.validacaodocumentos.exception.FileProcessingException;
import com.portodigital.validacaodocumentos.exception.UnsupportedFileFormatException;
import com.portodigital.validacaodocumentos.repository.DadosExtraidosRepository;
import com.portodigital.validacaodocumentos.repository.DocumentoRepository;
import com.portodigital.validacaodocumentos.repository.ValidacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestrador central do pipeline de validação de documentos (Facade Pattern).
 * Executa todas as 9 etapas desde a recepção multipart, persistência inicial,
 * tratamento de imagem via OpenCV, OCR Tess4J, estruturação com Spring AI,
 * validação das regras de negócio em Java puro e persistência transacional completa no PostgreSQL.
 */
@Service
public class DocumentPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DocumentPipelineOrchestrator.class);

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png",
            ".jpg",
            ".jpeg",
            ".pdf"
    );

    private final DocumentFileService documentFileService;
    private final ImagePreprocessingService imagePreprocessingService;
    private final OcrExtractionService ocrExtractionService;
    private final DocumentAiService documentAiService;
    private final DocumentValidationService documentValidationService;

    private final DocumentoRepository documentoRepository;
    private final DadosExtraidosRepository dadosExtraidosRepository;
    private final ValidacaoRepository validacaoRepository;

    public DocumentPipelineOrchestrator(
            DocumentFileService documentFileService,
            ImagePreprocessingService imagePreprocessingService,
            OcrExtractionService ocrExtractionService,
            DocumentAiService documentAiService,
            DocumentValidationService documentValidationService,
            DocumentoRepository documentoRepository,
            DadosExtraidosRepository dadosExtraidosRepository,
            ValidacaoRepository validacaoRepository) {
        this.documentFileService = documentFileService;
        this.imagePreprocessingService = imagePreprocessingService;
        this.ocrExtractionService = ocrExtractionService;
        this.documentAiService = documentAiService;
        this.documentValidationService = documentValidationService;
        this.documentoRepository = documentoRepository;
        this.dadosExtraidosRepository = dadosExtraidosRepository;
        this.validacaoRepository = validacaoRepository;
    }

    /**
     * Executa o pipeline completo de validação do documento.
     *
     * @param file          Arquivo enviado via multipart (PNG, JPEG, PDF)
     * @param tipoDocumento Tipo de documento (padrão COMPROVANTE_RESIDENCIA)
     * @return {@link ValidacaoResponseDTO} consolidado com status, critérios e dados extraídos
     */
    @Transactional
    public ValidacaoResponseDTO processarDocumento(MultipartFile file, TipoDocumento tipoDocumento) {
        log.info("Iniciando orquestração do pipeline para arquivo: '{}', tamanho: {} bytes",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);

        // 1. Validações preliminares do arquivo
        validarArquivo(file);

        TipoDocumento tipo = (tipoDocumento != null) ? tipoDocumento : TipoDocumento.COMPROVANTE_RESIDENCIA;

        // 2. Salva o registro inicial na tabela 'documentos'
        Documento documento = new Documento(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                tipo
        );
        documento = documentoRepository.save(documento);
        log.info("Etapa 2 concluída: Registro inicial persistido com ID: {}", documento.getId());

        // 3. Conversão para imagem e pré-processamento via OpenCV
        BufferedImage originalImage = documentFileService.convertToImage(file);
        BufferedImage preprocessedImage = imagePreprocessingService.preprocessImageToBufferedImage(originalImage);
        log.info("Etapa 3 concluída: Pré-processamento OpenCV finalizado");

        // 4. Extração de texto via Tesseract OCR e atualização de texto_bruto_ocr
        String textoBrutoOcr = ocrExtractionService.extractText(preprocessedImage);
        documento.setTextoBrutoOcr(textoBrutoOcr);
        documento = documentoRepository.save(documento);
        log.info("Etapa 4 concluída: Texto bruto extraído pelo OCR ({} caracteres) e persistido",
                textoBrutoOcr != null ? textoBrutoOcr.length() : 0);

        // 5. Envia o texto para o Spring AI e obtém o record DadosDocumento
        DadosDocumento dadosDocumento = documentAiService.extractDocumentData(textoBrutoOcr);
        log.info("Etapa 5 concluída: Estruturação com Spring AI finalizada");

        // 6. Salva os campos extraídos na tabela 'dados_extraidos'
        DadosExtraidos dadosExtraidos = new DadosExtraidos(
                documento,
                dadosDocumento.nome(),
                dadosDocumento.cpf(),
                dadosDocumento.endereco(),
                dadosDocumento.cidade(),
                dadosDocumento.estado(),
                dadosDocumento.dataEmissao()
        );
        dadosExtraidos = dadosExtraidosRepository.save(dadosExtraidos);
        documento.setDadosExtraidos(dadosExtraidos);
        log.info("Etapa 6 concluída: Dados extraídos persistidos com ID: {}", dadosExtraidos.getId());

        // 7. Executa o motor de validação de regras de negócio em Java puro
        ValidationResult validationResult = documentValidationService.validate(dadosDocumento, textoBrutoOcr);
        log.info("Etapa 7 concluída: Motor de regras executado. Status: {}", validationResult.status());

        // 8. Salva o resultado final na tabela 'validacoes'
        Validacao validacao = new Validacao(
                documento,
                validationResult.status(),
                validationResult.motivoReprovacao(),
                validationResult.criterios().documentoLegivel(),
                validationResult.criterios().cpfValido(),
                validationResult.criterios().enderecoEncontrado(),
                validationResult.criterios().emissaoMenor90Dias()
        );
        validacao = validacaoRepository.save(validacao);
        documento.setValidacao(validacao);
        log.info("Etapa 8 concluída: Validação final persistida com ID: {}", validacao.getId());

        // 9. Retorna o DTO consolidado para o cliente
        ValidacaoResponseDTO responseDTO = toDto(
                documento,
                dadosDocumento,
                validationResult.status(),
                validationResult.mensagem(),
                validationResult.motivoReprovacao(),
                validationResult.criterios(),
                validacao.getDataValidacao()
        );
        log.info("Etapa 9 concluída: Resposta compilada com sucesso para documento {}", documento.getId());
        return responseDTO;
    }

    /**
     * Consulta os detalhes de um documento já processado por seu identificador único.
     */
    @Transactional(readOnly = true)
    public ValidacaoResponseDTO buscarPorId(UUID id) {
        log.info("Buscando detalhes do documento por ID: {}", id);
        Documento documento = documentoRepository.findWithDetailsById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Documento não encontrado com o identificador: " + id));
        return toDto(documento);
    }

    /**
     * Lista o histórico de envios com paginação e filtro opcional por status.
     */
    @Transactional(readOnly = true)
    public Page<ValidacaoResponseDTO> listarEnvios(StatusValidacao status, Pageable pageable) {
        log.info("Listando histórico de envios. Filtro status: {}, Paginação: {}", status, pageable);
        return documentoRepository.findAllWithFilter(status, pageable)
                .map(this::toDto);
    }

    /**
     * Valida integridade básica, formato e limites de tamanho do arquivo enviado.
     */
    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("O arquivo enviado não pode ser nulo ou vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_FILE_SIZE_BYTES);
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean contentTypeValido = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));

        boolean extensaoValida = false;
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase(Locale.ROOT);
            extensaoValida = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        }

        if (!contentTypeValido && !extensaoValida) {
            throw new UnsupportedFileFormatException(
                    "Formato de arquivo não suportado ('" + contentType + "'). São permitidos apenas arquivos PNG, JPEG ou PDF."
            );
        }
    }

    /**
     * Converte entidade Documento completa para o DTO de resposta da API.
     */
    public ValidacaoResponseDTO toDto(Documento doc) {
        DadosExtraidos de = doc.getDadosExtraidos();
        Validacao v = doc.getValidacao();

        DadosDocumento dados = (de != null)
                ? new DadosDocumento(de.getNomeTitular(), de.getCpf(), de.getEnderecoCompleto(), de.getCidade(), de.getEstado(), de.getDataEmissao())
                : null;

        CriteriosValidacao criterios = (v != null)
                ? new CriteriosValidacao(
                        dados != null && dados.cpf() != null && !dados.cpf().isBlank(),
                        v.isCpfValido(),
                        v.isEnderecoValido(),
                        v.isLegivel(),
                        v.isEmissaoRecente()
                )
                : null;

        StatusValidacao status = (v != null) ? v.getStatus() : null;
        String motivo = (v != null) ? v.getMotivoReprovacao() : null;
        String mensagem = (status == StatusValidacao.APROVADO)
                ? "Documento validado com sucesso."
                : (status == StatusValidacao.REPROVADO)
                        ? "Documento reprovado nas regras de validação."
                        : "Documento em processamento.";

        OffsetDateTime dataProcessamento = (v != null && v.getDataValidacao() != null)
                ? v.getDataValidacao()
                : doc.getDataUpload();

        return new ValidacaoResponseDTO(
                doc.getId(),
                doc.getNomeArquivoOriginal(),
                doc.getTipoDocumento(),
                status,
                mensagem,
                motivo,
                criterios,
                dados,
                dataProcessamento
        );
    }

    private ValidacaoResponseDTO toDto(
            Documento doc,
            DadosDocumento dados,
            StatusValidacao status,
            String mensagem,
            String motivoReprovacao,
            CriteriosValidacao criterios,
            OffsetDateTime dataProcessamento) {
        return new ValidacaoResponseDTO(
                doc.getId(),
                doc.getNomeArquivoOriginal(),
                doc.getTipoDocumento(),
                status,
                mensagem,
                motivoReprovacao,
                criterios,
                dados,
                dataProcessamento != null ? dataProcessamento : OffsetDateTime.now()
        );
    }
}
