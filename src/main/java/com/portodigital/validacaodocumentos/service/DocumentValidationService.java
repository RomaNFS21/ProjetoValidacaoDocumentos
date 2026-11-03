package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.CriteriosValidacao;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.service.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de Validação de Regras de Negócio de Documentos (Padrão Strategy).
 * Avalia cumulativamente as 4 regras:
 * 1. Presença e Validade do CPF (Módulo 11 e dígitos repetidos)
 * 2. Presença de Endereço (logradouro e numeração identificáveis)
 * 3. Legibilidade Geral do Documento (comprimento de texto e campos essenciais)
 * 4. Recência Temporal (< 90 dias, não nula, não futura)
 */
@Service
public class DocumentValidationService implements RegrasValidacaoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentValidationService.class);

    private final CpfValidationRule cpfValidationRule;
    private final EnderecoValidationRule enderecoValidationRule;
    private final LegibilidadeValidationRule legibilidadeValidationRule;
    private final RecenciaTemporalValidationRule recenciaTemporalValidationRule;

    public DocumentValidationService(
            CpfValidationRule cpfValidationRule,
            EnderecoValidationRule enderecoValidationRule,
            LegibilidadeValidationRule legibilidadeValidationRule,
            RecenciaTemporalValidationRule recenciaTemporalValidationRule) {
        this.cpfValidationRule = cpfValidationRule;
        this.enderecoValidationRule = enderecoValidationRule;
        this.legibilidadeValidationRule = legibilidadeValidationRule;
        this.recenciaTemporalValidationRule = recenciaTemporalValidationRule;
    }

    /**
     * Construtor padrão com instâncias diretas das regras (útil para testes unitários isolados).
     */
    public DocumentValidationService() {
        this(
                new CpfValidationRule(),
                new EnderecoValidationRule(),
                new LegibilidadeValidationRule(),
                new RecenciaTemporalValidationRule()
        );
    }

    @Override
    public ValidationResult validar(DadosDocumento dados, String textoOcr) {
        return validate(dados, textoOcr);
    }

    /**
     * Executa a validação de conformidade do documento e determina o status final.
     *
     * @param dados   Dados extraídos estruturados
     * @param rawText Texto bruto extraído pelo OCR
     * @return {@link ValidationResult} com status final, critérios e eventuais motivos de reprovação
     */
    public ValidationResult validate(DadosDocumento dados, String rawText) {
        log.info("Iniciando validação de regras de negócio para documento...");

        boolean cpfPresente = (dados != null && dados.cpf() != null && !dados.cpf().trim().isEmpty());
        boolean enderecoPresente = (dados != null && dados.endereco() != null && !dados.endereco().trim().isEmpty());

        List<String> motivosReprovacao = new ArrayList<>();

        // 1. Regra 1: CPF
        ValidationRuleResult resCpf = cpfValidationRule.validate(dados, rawText);
        boolean cpfValido = resCpf.aprovado();
        if (!cpfValido) {
            motivosReprovacao.add(resCpf.motivoReprovacao());
        }

        // 2. Regra 2: Endereço
        ValidationRuleResult resEndereco = enderecoValidationRule.validate(dados, rawText);
        boolean enderecoValido = resEndereco.aprovado();
        if (!enderecoValido) {
            motivosReprovacao.add(resEndereco.motivoReprovacao());
        }

        // 3. Regra 3: Legibilidade Geral
        ValidationRuleResult resLegibilidade = legibilidadeValidationRule.validate(dados, rawText);
        boolean documentoLegivel = resLegibilidade.aprovado();
        if (!documentoLegivel) {
            motivosReprovacao.add(resLegibilidade.motivoReprovacao());
        }

        // 4. Regra 4: Recência Temporal (< 90 dias)
        ValidationRuleResult resRecencia = recenciaTemporalValidationRule.validate(dados, rawText);
        boolean emissaoMenor90Dias = resRecencia.aprovado();
        if (!emissaoMenor90Dias) {
            motivosReprovacao.add(resRecencia.motivoReprovacao());
        }

        CriteriosValidacao criterios = new CriteriosValidacao(
                cpfPresente,
                cpfValido,
                enderecoValido,
                documentoLegivel,
                emissaoMenor90Dias
        );

        boolean aprovado = criterios.isTodosAprovados();
        StatusValidacao status = aprovado ? StatusValidacao.APROVADO : StatusValidacao.REPROVADO;

        String mensagem = aprovado
                ? "Documento validado com sucesso."
                : "Documento reprovado nas regras de validação.";

        String motivoFinal = aprovado ? null : String.join("; ", motivosReprovacao);

        log.info("Resultado da validação: Status = {}, Motivos = {}", status, motivoFinal);

        return new ValidationResult(status, mensagem, motivoFinal, criterios, motivosReprovacao);
    }
}
