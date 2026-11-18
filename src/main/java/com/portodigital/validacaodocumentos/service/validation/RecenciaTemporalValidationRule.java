package com.portodigital.validacaodocumentos.service.validation;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regra 4 - Recência Temporal / Validade do Documento:
 * - A dataEmissao informada não pode ser nula nem futura.
 * - Para Comprovante de Residência: diferença entre hoje e dataEmissao < 90 dias.
 * - Para CNH e RG: diferença entre hoje e dataEmissao <= 10 anos.
 */
@Component
public class RecenciaTemporalValidationRule implements DocumentValidationRule {

    private static final String REGRA_NOME = "RecenciaTemporal";
    private static final long LIMITE_DIAS_COMPROVANTE = 90L;
    private static final long LIMITE_ANOS_DOCUMENTO_IDENTIFICACAO = 10L;

    @Override
    public String getNomeRegra() {
        return REGRA_NOME;
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText) {
        return validate(dados, rawText, TipoDocumento.COMPROVANTE_RESIDENCIA);
    }

    @Override
    public ValidationRuleResult validate(DadosDocumento dados, String rawText, TipoDocumento tipoDocumento) {
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

        TipoDocumento tipo = (tipoDocumento != null) ? tipoDocumento : TipoDocumento.COMPROVANTE_RESIDENCIA;

        // Se for CNH ou RG, a validade máxima de emissão é de 10 anos
        if (tipo == TipoDocumento.CNH || tipo == TipoDocumento.RG) {
            LocalDate limite10AnosAtras = hoje.minusYears(LIMITE_ANOS_DOCUMENTO_IDENTIFICACAO);
            if (dataEmissao.isBefore(limite10AnosAtras)) {
                long anos = ChronoUnit.YEARS.between(dataEmissao, hoje);
                return ValidationRuleResult.reprovado(
                        REGRA_NOME,
                        "Data de emissão superior a 10 anos para " + tipo.name() + ": emitida há " + anos + " anos (emitido em " + dataEmissao + ")"
                );
            }
            return ValidationRuleResult.aprovado(REGRA_NOME);
        }

        // Padrão: Comprovante de Residência (< 90 dias)
        long dias = ChronoUnit.DAYS.between(dataEmissao, hoje);
        if (dias >= LIMITE_DIAS_COMPROVANTE) {
            return ValidationRuleResult.reprovado(
                    REGRA_NOME,
                    "Data de emissão superior a 90 dias: " + dias + " dias atrás (emitido em " + dataEmissao + ")"
            );
        }

        return ValidationRuleResult.aprovado(REGRA_NOME);
    }
}
