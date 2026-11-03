package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;

/**
 * Interface Strategy para regras de negócio de validação de documentos.
 */
public interface DocumentValidationRule {

    String getNomeRegra();

    ValidationRuleResult validate(DadosDocumento dados, String rawText);
}
