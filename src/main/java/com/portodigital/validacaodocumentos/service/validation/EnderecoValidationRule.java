package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Regra 2 - Presença de Endereço:
 * O campo de endereço deve estar preenchido com logradouro e numeração identificáveis.
 */
@Component
public class EnderecoValidationRule implements DocumentValidationRule {

    private static final String REGRA_NOME = "PresencaEndereco";

    // Padrão que detecta numeração residencial (dígitos ou indicações de 'sem número')
    private static final Pattern NUMERACAO_PATTERN = Pattern.compile(
            "(\\b\\d+\\b|\\bS/N\\b|\\bSN\\b|\\bSEM\\s+N[UÚ]MERO\\b)",
            Pattern.CASE_INSENSITIVE
    );

    // Padrão que detecta logradouro mínimo identificável (ao menos uma palavra com 3+ letras)
    private static final Pattern LOGRADOURO_PATTERN = Pattern.compile(
            "[a-zA-ZáéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ]{3,}"
    );

    @Override
    public String getNomeRegra() {
        return REGRA_NOME;
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText) {
        if (dados == null || dados.endereco() == null || dados.endereco().trim().isEmpty()) {
            return ValidationRuleResult.reprovado(REGRA_NOME, "Endereço ausente no documento");
        }

        String endereco = dados.endereco().trim();

        if (endereco.length() < 5) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Endereço incompleto: texto muito curto ou insuficiente"
            );
        }

        boolean temLogradouro = LOGRADOURO_PATTERN.matcher(endereco).find();
        boolean temNumeracao = NUMERACAO_PATTERN.matcher(endereco).find();

        if (!temLogradouro || !temNumeracao) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Endereço incompleto ou com numeração/logradouro não identificáveis (" + endereco + ")"
            );
        }

        return ValidationRuleResult.aprovado(REGRA_NOME);
    }
}
