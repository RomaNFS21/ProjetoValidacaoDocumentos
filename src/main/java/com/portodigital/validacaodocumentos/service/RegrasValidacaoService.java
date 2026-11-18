package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;

/**
 * Contrato de serviço para execução do motor de validação de regras de negócio.
 */
public interface RegrasValidacaoService {
    ValidationResult validar(DadosDocumento dados, String textoOcr);

    default ValidationResult validar(DadosDocumento dados, String textoOcr, TipoDocumento tipoDocumento) {
        return validar(dados, textoOcr);
    }
}
