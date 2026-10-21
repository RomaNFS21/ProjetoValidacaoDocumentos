package com.portodigital.validacaodocumentos.domain.enums;

public enum StatusValidacao {
    APROVADO("Aprovado"),
    REPROVADO("Reprovado");

    private final String descricao;

    StatusValidacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
