package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import org.springframework.stereotype.Component;

/**
 * Regra 1 - Presença e Validade do CPF:
 * - O CPF deve estar preenchido.
 * - Implementa o algoritmo oficial de validação dos dois dígitos verificadores (módulo 11).
 * - Rejeita sequências de dígitos repetidos (ex: 111.111.111-11).
 */
@Component
public class CpfValidationRule implements DocumentValidationRule {

    private static final String REGRA_NOME = "PresencaEValidadeCpf";

    @Override
    public String getNomeRegra() {
        return REGRA_NOME;
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText) {
        if (dados == null || dados.cpf() == null || dados.cpf().trim().isEmpty()) {
            return ValidationRuleResult.reprovado(REGRA_NOME, "CPF ausente no documento");
        }

        String digitsOnly = dados.cpf().replaceAll("\\D", "");

        if (digitsOnly.length() != 11) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "CPF inválido: quantidade incorreta de dígitos (" + digitsOnly.length() + " dígitos encontrados)"
            );
        }

        // Verifica se é uma sequência de dígitos repetidos (ex: 00000000000, 11111111111)
        if (digitsOnly.chars().distinct().count() == 1) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "CPF inválido: sequência de dígitos repetidos detectada (" + dados.cpf() + ")"
            );
        }

        if (!validarDigitosVerificadores(digitsOnly)) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "CPF ausente ou com dígitos verificadores inválidos (" + dados.cpf() + ")"
            );
        }

        return ValidationRuleResult.aprovado(REGRA_NOME);
    }

    /**
     * Validação oficial do CPF pelo algoritmo do Módulo 11 da Receita Federal.
     */
    public static boolean validarDigitosVerificadores(String digitsOnly) {
        if (digitsOnly == null || digitsOnly.length() != 11) {
            return false;
        }

        try {
            // Primeiro dígito verificador
            int soma1 = 0;
            for (int i = 0; i < 9; i++) {
                int digito = Character.getNumericValue(digitsOnly.charAt(i));
                soma1 += digito * (10 - i);
            }
            int resto1 = 11 - (soma1 % 11);
            int dv1 = (resto1 >= 10) ? 0 : resto1;

            if (dv1 != Character.getNumericValue(digitsOnly.charAt(9))) {
                return false;
            }

            // Segundo dígito verificador
            int soma2 = 0;
            for (int i = 0; i < 10; i++) {
                int digito = Character.getNumericValue(digitsOnly.charAt(i));
                soma2 += digito * (11 - i);
            }
            int resto2 = 11 - (soma2 % 11);
            int dv2 = (resto2 >= 10) ? 0 : resto2;

            return dv2 == Character.getNumericValue(digitsOnly.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
}
