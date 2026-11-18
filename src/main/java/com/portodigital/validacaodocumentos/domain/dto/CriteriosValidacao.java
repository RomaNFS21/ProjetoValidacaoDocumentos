package com.portodigital.validacaodocumentos.domain.dto;

import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;

public record CriteriosValidacao(
    boolean cpfEncontrado,
    boolean cpfValido,
    boolean enderecoEncontrado,
    boolean documentoLegivel,
    boolean emissaoMenor90Dias
) {
    public boolean isTodosAprovados() {
        return cpfEncontrado && cpfValido && enderecoEncontrado && documentoLegivel && emissaoMenor90Dias;
    }

    public boolean isTodosAprovados(TipoDocumento tipoDocumento) {
        if (tipoDocumento == TipoDocumento.CNH || tipoDocumento == TipoDocumento.RG) {
            return cpfEncontrado && cpfValido && documentoLegivel && emissaoMenor90Dias;
        }
        return isTodosAprovados();
    }
}
