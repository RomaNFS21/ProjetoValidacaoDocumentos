package com.portodigital.validacaodocumentos.domain.dto;

import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ValidacaoResponseDTO(
    UUID documentoId,
    String nomeArquivo,
    TipoDocumento tipoDocumento,
    StatusValidacao status,
    String mensagem,
    String motivoReprovacao,
    CriteriosValidacao criterios,
    DadosDocumento dadosExtraidos,
    OffsetDateTime dataProcessamento
) {}
