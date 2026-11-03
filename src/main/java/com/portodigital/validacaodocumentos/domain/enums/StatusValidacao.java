package com.portodigital.validacaodocumentos.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusValidacao {
    EM_PROCESSAMENTO("Em Processamento"),
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
