package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.CriteriosValidacao;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.DadosExtraidosDTO;
import com.portodigital.validacaodocumentos.domain.dto.DocumentoRespostaDTO;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;
import com.portodigital.validacaodocumentos.domain.entity.CriteriosValidacaoEmbeddable;
import com.portodigital.validacaodocumentos.domain.entity.DadosExtraidos;
import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.entity.TextoBrutoOcr;
import com.portodigital.validacaodocumentos.domain.entity.Validacao;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import com.portodigital.validacaodocumentos.exception.DocumentNotFoundException;
import com.portodigital.validacaodocumentos.exception.FileProcessingException;
import com.portodigital.validacaodocumentos.exception.UnsupportedFileFormatException;
import com.portodigital.validacaodocumentos.repository.DadosExtraidosRepository;
import com.portodigital.validacaodocumentos.repository.DocumentoRepository;
import com.portodigital.validacaodocumentos.repository.TextoBrutoOcrRepository;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestrador central do pipeline de validação documental (Facade Pattern).
 * Coordena as etapas de persistência, visão computacional, OCR, IA generativa e motor de regras de negócio.
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

    // Serviços secundários do pipeline
    private final OpenCvService openCvService;
    private final TesseractOcrService tesseractOcrService;
    private final SpringAiService springAiService;
    private final RegrasValidacaoService regrasValidacaoService;

    // Repositórios JPA para persistência de todo o ciclo de vida
    private final DocumentoRepository documentoRepository;
    private final TextoBrutoOcrRepository textoBrutoOcrRepository;
    private final DadosExtraidosRepository dadosExtraidosRepository;
    private final ValidacaoRepository validacaoRepository;

    public DocumentPipelineOrchestrator(
            OpenCvService openCvService,
            TesseractOcrService tesseractOcrService,
            SpringAiService springAiService,
            RegrasValidacaoService regrasValidacaoService,
            DocumentoRepository documentoRepository,
            TextoBrutoOcrRepository textoBrutoOcrRepository,
            DadosExtraidosRepository dadosExtraidosRepository,
            ValidacaoRepository validacaoRepository) {
        this.openCvService = openCvService;
        this.tesseractOcrService = tesseractOcrService;
        this.springAiService = springAiService;
        this.regrasValidacaoService = regrasValidacaoService;
        this.documentoRepository = documentoRepository;
        this.textoBrutoOcrRepository = textoBrutoOcrRepository;
        this.dadosExtraidosRepository = dadosExtraidosRepository;
        this.validacaoRepository = validacaoRepository;
    }

    /**
     * Executa o pipeline completo de validação do documento em uma única transação atômica.
     *
     * @param file          Arquivo binário recebido via Multipart (PNG, JPEG ou PDF)
     * @param tipo          Tipo de documento submetido (padrão COMPROVANTE_RESIDENCIA)
     * @return {@link DocumentoRespostaDTO} DTO consolidado com dados extraídos, critérios e status
     */
    @Transactional
    public DocumentoRespostaDTO processarDocumento(MultipartFile file, TipoDocumento tipo) {
        log.info("Iniciando orquestração do pipeline para arquivo: '{}', tamanho: {} bytes",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0);

        // Validação preliminar de integridade, limites e formato
        validarArquivo(file);
        TipoDocumento tipoDoc = (tipo != null) ? tipo : TipoDocumento.COMPROVANTE_RESIDENCIA;

        // 1. Salva o registro inicial na tabela 'documentos' (Status: EM_PROCESSAMENTO)
        Documento documento = new Documento(file.getOriginalFilename(), tipoDoc, StatusValidacao.EM_PROCESSAMENTO);
        documento = documentoRepository.save(documento);
        log.info("[Etapa 1/9] Registro inicial persistido com ID: {} e Status: {}", documento.getId(), documento.getStatus());

        // 2. Executa a conversão e pré-processamento via OpenCV
        BufferedImage imagemProcessada = openCvService.preprocessar(file);
        log.info("[Etapa 2/9] Pré-processamento OpenCV concluído com sucesso");

        // 3. Executa a extração de texto via Tesseract OCR e salva/atualiza na tabela 'texto_bruto_ocr'
        String textoBrutoOcr = tesseractOcrService.extrairTexto(imagemProcessada);
        TextoBrutoOcr textoOcrEntity = new TextoBrutoOcr(documento, textoBrutoOcr);
        textoOcrEntity = textoBrutoOcrRepository.save(textoOcrEntity);
        documento.setTextoBrutoOcr(textoOcrEntity);
        log.info("[Etapa 3/9] OCR finalizado ({} caracteres). Registro salvo em 'texto_bruto_ocr'",
                textoBrutoOcr != null ? textoBrutoOcr.length() : 0);

        // 4. Envia o texto ao Spring AI e obtém o record DadosDocumento
        DadosDocumento dadosDocumento = springAiService.extrairDados(textoBrutoOcr);
        log.info("[Etapa 4/9] Extração estruturada via Spring AI finalizada");

        // 5. Salva os campos extraídos na tabela 'dados_extraidos'
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
        log.info("[Etapa 5/9] Campos extraídos persistidos em 'dados_extraidos' com ID: {}", dadosExtraidos.getId());

        // 6. Executa o motor de validação de regras de negócio
        ValidationResult validationResult = regrasValidacaoService.validar(dadosDocumento, textoBrutoOcr);
        log.info("[Etapa 6/9] Motor de regras executado. Resultado: {}", validationResult.status());

        // 7. Salva o resultado final na tabela 'validacoes' (Status APROVADO ou REPROVADO + mensagem + critérios)
        CriteriosValidacaoEmbeddable criteriosEmbeddable = CriteriosValidacaoEmbeddable.builder()
                .cpfEncontrado(validationResult.criterios().cpfEncontrado())
                .cpfValido(validationResult.criterios().cpfValido())
                .enderecoEncontrado(validationResult.criterios().enderecoEncontrado())
                .documentoLegivel(validationResult.criterios().documentoLegivel())
                .emissaoMenor90Dias(validationResult.criterios().emissaoMenor90Dias())
                .build();

        Validacao validacao = new Validacao(
                documento,
                validationResult.status(),
                validationResult.motivoReprovacao(),
                criteriosEmbeddable
        );
        validacao = validacaoRepository.save(validacao);
        documento.setValidacao(validacao);
        log.info("[Etapa 7/9] Validação persistida com ID: {} e Status: {}", validacao.getId(), validacao.getStatus());

        // 8. Atualiza o status do documento em 'documentos'
        documento.setStatus(validationResult.status());
        documento = documentoRepository.save(documento);
        log.info("[Etapa 8/9] Status do documento atualizado em 'documentos' para: {}", documento.getStatus());

        // 9. Monta e retorna o DocumentoRespostaDTO consolidado
        CriteriosValidacao criteriosDto = validationResult.criterios();
        DadosExtraidosDTO dadosDto = new DadosExtraidosDTO(
                dadosDocumento.nome(),
                dadosDocumento.cpf(),
                dadosDocumento.endereco(),
                dadosDocumento.cidade(),
                dadosDocumento.estado(),
                dadosDocumento.dataEmissao()
        );

        DocumentoRespostaDTO responseDTO = new DocumentoRespostaDTO(
                documento.getId(),
                documento.getNomeArquivo(),
                documento.getTipoDocumento(),
                documento.getStatus(),
                validationResult.mensagem(),
                validationResult.motivoReprovacao(),
                criteriosDto,
                dadosDto,
                validacao.getDataProcessamento()
        );

        log.info("[Etapa 9/9] Pipeline finalizado com sucesso. Documento: {}, Status: {}", documento.getId(), documento.getStatus());
        return responseDTO;
    }

    /**
     * Consulta os detalhes de um documento já processado por seu UUID.
     */
    @Transactional(readOnly = true)
    public DocumentoRespostaDTO buscarPorId(UUID id) {
        log.info("Buscando detalhes do documento por ID: {}", id);
        Documento documento = documentoRepository.findWithDetailsById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Documento não encontrado com o identificador: " + id));
        return DocumentoRespostaDTO.fromEntity(documento);
    }

    /**
     * Lista o histórico de envios com paginação e filtro opcional por status.
     */
    @Transactional(readOnly = true)
    public Page<DocumentoRespostaDTO> listarEnvios(StatusValidacao status, Pageable pageable) {
        log.info("Listando histórico de envios. Filtro status: {}, Paginação: {}", status, pageable);
        Page<Documento> page = (status != null)
                ? documentoRepository.findByStatus(status, pageable)
                : documentoRepository.findAll(pageable);
        return page.map(DocumentoRespostaDTO::fromEntity);
    }

    /**
     * Valida integridade, formato suportado e limite máximo de tamanho de arquivo (10 MB).
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
}
