package com.portodigital.validacaodocumentos.domain.dto;

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
}
