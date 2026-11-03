package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;

/**
 * Contrato de serviço para execução do motor de validação de regras de negócio.
 */
public interface RegrasValidacaoService {
    ValidationResult validar(DadosDocumento dados, String textoOcr);
}

