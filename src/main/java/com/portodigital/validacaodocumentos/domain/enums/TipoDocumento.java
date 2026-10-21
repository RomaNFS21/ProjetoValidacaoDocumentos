package com.portodigital.validacaodocumentos.domain.enums;

public enum TipoDocumento {
    COMPROVANTE_RESIDENCIA("Comprovante de Residência"),
    RG("Registro Geral (RG)"),
    CNH("Carteira Nacional de Habilitação (CNH)");

    private final String descricao;

    TipoDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
