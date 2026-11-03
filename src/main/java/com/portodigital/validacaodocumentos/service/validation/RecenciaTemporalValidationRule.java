package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regra 4 - Recência Temporal (< 90 dias):
 * - A dataEmissao informada não pode ser nula nem futura.
 * - A diferença entre a data da análise (LocalDate.now()) e a dataEmissao deve ser ESTRITAMENTE MENOR que 90 dias:
 *   ChronoUnit.DAYS.between(dataEmissao, hoje) < 90.
 */
@Component
public class RecenciaTemporalValidationRule implements DocumentValidationRule {

    private static final String REGRA_NOME = "RecenciaTemporal";
    private static final long LIMITE_DIAS_VALIDADE = 90L;

    @Override
    public String getNomeRegra() {
        return REGRA_NOME;
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText) {
        if (dados == null || dados.dataEmissao() == null) {
            return ValidationRuleResult.reprovado(REGRA_NOME, "Data de emissão ausente no documento");
        }

        LocalDate dataEmissao = dados.dataEmissao();
        LocalDate hoje = LocalDate.now();

        if (dataEmissao.isAfter(hoje)) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Data de emissão futura inválida: documento datado em " + dataEmissao + ", data atual: " + hoje
            );
        }

        long dias = ChronoUnit.DAYS.between(dataEmissao, hoje);

        if (dias >= LIMITE_DIAS_VALIDADE) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Data de emissão superior a 90 dias: " + dias + " dias atrás (emitido em " + dataEmissao + ")"
            );
        }

        return ValidationRuleResult.aprovado(REGRA_NOME);
    }
}
