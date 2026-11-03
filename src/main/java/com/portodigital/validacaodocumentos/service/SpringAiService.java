package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;

/**
 * Contrato de serviço para estruturação de dados de documentos via Spring AI / LLM.
 */
public interface SpringAiService {
    DadosDocumento extrairDados(String textoOcr);
}

