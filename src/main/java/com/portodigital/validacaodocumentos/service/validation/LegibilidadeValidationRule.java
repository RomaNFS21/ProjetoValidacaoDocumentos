package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import org.springframework.stereotype.Component;

/**
 * Regra 3 - Legibilidade Geral do Documento:
 * - O texto bruto extraído deve conter tamanho mínimo (ao menos 30 caracteres).
 * - Os campos essenciais (nome, cpf, endereco) não podem ser todos nulos ou vazios.
 */
@Component
public class LegibilidadeValidationRule implements DocumentValidationRule {

    private static final String REGRA_NOME = "LegibilidadeGeral";
    private static final int TAMANHO_MINIMO_TEXTO_BRUTO = 30;

    @Override
    public String getNomeRegra() {
        return REGRA_NOME;
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText) {
        if (rawText == null || rawText.trim().length() < TAMANHO_MINIMO_TEXTO_BRUTO) {
            int tamanhoAtual = (rawText == null) ? 0 : rawText.trim().length();
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Documento ilegível ou com texto OCR insuficiente (" + tamanhoAtual + " caracteres, mínimo de " + TAMANHO_MINIMO_TEXTO_BRUTO + ")"
            );
        }

        // Verifica se todos os campos essenciais foram nulos ou vazios
        boolean nomeAusente = (dados == null || dados.nome() == null || dados.nome().trim().isEmpty());
        boolean cpfAusente = (dados == null || dados.cpf() == null || dados.cpf().trim().isEmpty());
        boolean enderecoAusente = (dados == null || dados.endereco() == null || dados.endereco().trim().isEmpty());

        if (nomeAusente && cpfAusente && enderecoAusente) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Documento com qualidade de leitura comprometida: nenhum campo essencial pôde ser identificado"
            );
        }

        return ValidationRuleResult.aprovado(REGRA_NOME);
    }
}
