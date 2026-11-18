package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;

/**
 * Interface Strategy para regras de negócio de validação de documentos.
 */
public interface DocumentValidationRule {

    String getNomeRegra();

    ValidationRuleResult validate(DadosDocumento dados, String rawText);

    default ValidationRuleResult validate(DadosDocumento dados, String rawText, TipoDocumento tipoDocumento) {
        return validate(dados, rawText);
    }
}
