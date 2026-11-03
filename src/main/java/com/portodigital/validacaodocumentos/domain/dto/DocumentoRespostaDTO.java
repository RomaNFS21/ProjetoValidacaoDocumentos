package com.portodigital.validacaodocumentos.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.portodigital.validacaodocumentos.domain.entity.Documento;
import com.portodigital.validacaodocumentos.domain.entity.Validacao;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record DTO de saída consolidado para representação de resposta da API de validação documental.
 */
public record DocumentoRespostaDTO(
    UUID documentoId,
    String nomeArquivo,
    TipoDocumento tipoDocumento,
    StatusValidacao status,
    String mensagem,
    String motivoReprovacao,
    CriteriosValidacao criterios,
    DadosExtraidosDTO dadosExtraidos,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataProcessamento
) {
    public DocumentoRespostaDTO(
            UUID documentoId,
            String nomeArquivo,
            TipoDocumento tipoDocumento,
            StatusValidacao status,
            String mensagem,
            CriteriosValidacao criterios,
            DadosExtraidosDTO dadosExtraidos,
            LocalDateTime dataProcessamento) {
        this(documentoId, nomeArquivo, tipoDocumento, status, mensagem, null, criterios, dadosExtraidos, dataProcessamento);
    }

    /**
     * Constrói o DTO a partir da entidade JPA {@link Documento} com suas associações carregadas.
     */
    public static DocumentoRespostaDTO fromEntity(Documento documento) {
        if (documento == null) {
            return null;
        }

        Validacao validacao = documento.getValidacao();

        CriteriosValidacao criteriosDto = null;
        if (validacao != null && validacao.getCriterios() != null) {
            var c = validacao.getCriterios();
            criteriosDto = new CriteriosValidacao(
                c.isCpfEncontrado(),
                c.isCpfValido(),
                c.isEnderecoEncontrado(),
                c.isDocumentoLegivel(),
                c.isEmissaoMenor90Dias()
            );
        }

        DadosExtraidosDTO dadosDto = DadosExtraidosDTO.fromEntity(documento.getDadosExtraidos());

        String motivo = (validacao != null) ? validacao.getMensagem() : null;
        String mensagem = (validacao != null && validacao.getMensagem() != null)
                ? validacao.getMensagem()
                : (documento.getStatus() == StatusValidacao.APROVADO
                    ? "Documento validado com sucesso."
                    : (documento.getStatus() == StatusValidacao.REPROVADO
                        ? "Documento reprovado nas regras de validação."
                        : "Documento em processamento."));

        LocalDateTime dataProc = (validacao != null && validacao.getDataProcessamento() != null)
                ? validacao.getDataProcessamento()
                : documento.getDataCriacao();

        return new DocumentoRespostaDTO(
            documento.getId(),
            documento.getNomeArquivo(),
            documento.getTipoDocumento(),
            documento.getStatus(),
            mensagem,
            motivo,
            criteriosDto,
            dadosDto,
            dataProc
        );
    }
}
