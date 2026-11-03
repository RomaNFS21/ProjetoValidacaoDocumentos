package com.portodigital.validacaodocumentos.service.validation;

/**
 * Resultado individual da avaliação de uma regra de validação.
 */
public record ValidationRuleResult(
        String nomeRegra,
        boolean aprovado,
        String motivoReprovacao
) {
    public static ValidationRuleResult aprovado(String nomeRegra) {
        return new ValidationRuleResult(nomeRegra, true, null);
    }

    public static ValidationRuleResult reprovado(String nomeRegra, String motivoReprovacao) {
        return new ValidationRuleResult(nomeRegra, false, motivoReprovacao);
    }
}
