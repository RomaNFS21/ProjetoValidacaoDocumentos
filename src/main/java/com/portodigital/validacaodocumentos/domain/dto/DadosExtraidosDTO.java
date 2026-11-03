package com.portodigital.validacaodocumentos.domain.dto;

import java.time.LocalDate;

/**
 * Record DTO com os dados estruturados extraídos do documento.
 */
public record DadosExtraidosDTO(
    String nome,
    String cpf,
    String endereco,
    String cidade,
    String estado,
    LocalDate dataEmissao
) {
    public static DadosExtraidosDTO fromEntity(com.portodigital.validacaodocumentos.domain.entity.DadosExtraidos entity) {
        if (entity == null) {
            return null;
        }
        return new DadosExtraidosDTO(
            entity.getNome(),
            entity.getCpf(),
            entity.getEndereco(),
            entity.getCidade(),
            entity.getEstado(),
            entity.getDataEmissao()
        );
    }
}

