package com.portodigital.validacaodocumentos.domain.dto;

import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;

import java.util.List;

/**
 * Resultado compilado da execução do motor de regras de negócio.
 */
public record ValidationResult(
        StatusValidacao status,
        String mensagem,
        String motivoReprovacao,
        CriteriosValidacao criterios,
        List<String> falhas
) {}
