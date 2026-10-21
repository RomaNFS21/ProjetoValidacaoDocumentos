package com.portodigital.validacaodocumentos.domain.dto;

import java.time.LocalDate;

public record DadosDocumento(
    String nome,
    String cpf,
    String endereco,
    String cidade,
    String estado,
    LocalDate dataEmissao
) {}
