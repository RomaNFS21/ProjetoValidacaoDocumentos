package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.domain.dto.ValidationResult;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes Unitários do Motor de Validação de Regras de Negócio (JUnit 5 e AssertJ).
 */
class DocumentValidationServiceTest {

    private DocumentValidationService validationService;

    // CPF válido com algoritmo módulo 11 da Receita Federal
    private static final String CPF_VALIDO = "529.982.247-25";
    private static final String TEXTO_BRUTO_LEGIVEL = """
            COMPROVANTE DE RESIDENCIA
            Companhia de Eletricidade e Servicos
            Nome: Joao da Silva
            CPF: 529.982.247-25
            Endereco: Rua das Flores, 150
            Bairro: Centro
            Cidade: Guanambi - BA
            Data de emissao: 15/09/2026
            Codigo de Instalacao: 00987654321
            """;

    @BeforeEach
    void setUp() {
        validationService = new DocumentValidationService();
    }

    @Test
    @DisplayName("Cenário 1: Documento com todos os dados válidos deve retornar status APROVADO")
    void shouldApproveWhenAllRulesAreSatisfied() {
        // Data recente: 15 dias atrás (< 90 dias)
        LocalDate dataRecente = LocalDate.now().minusDays(15);

        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                dataRecente
        );

        ValidationResult result = validationService.validate(dados, TEXTO_BRUTO_LEGIVEL);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(StatusValidacao.APROVADO);
        assertThat(result.mensagem()).isEqualTo("Documento validado com sucesso.");
        assertThat(result.motivoReprovacao()).isNull();
        assertThat(result.falhas()).isEmpty();

        // Checagem dos critérios individuais
        assertThat(result.criterios().cpfEncontrado()).isTrue();
        assertThat(result.criterios().cpfValido()).isTrue();
        assertThat(result.criterios().enderecoEncontrado()).isTrue();
        assertThat(result.criterios().documentoLegivel()).isTrue();
        assertThat(result.criterios().emissaoMenor90Dias()).isTrue();
        assertThat(result.criterios().isTodosAprovados()).isTrue();
    }

    @Test
    @DisplayName("Cenário 2: CPF com dígitos verificadores incorretos deve reprovar o documento")
    void shouldRejectWhenCpfCheckDigitsAreInvalid() {
        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                "123.456.789-00", // Dígitos verificadores incorretos
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(20)
        );

        ValidationResult result = validationService.validate(dados, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().cpfValido()).isFalse();
        assertThat(result.criterios().isTodosAprovados()).isFalse();
        assertThat(result.motivoReprovacao()).contains("CPF ausente ou com dígitos verificadores inválidos");
    }

    @Test
    @DisplayName("Cenário 3: CPF com sequência de dígitos repetidos deve reprovar o documento")
    void shouldRejectWhenCpfHasRepeatedDigits() {
        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                "111.111.111-11", // Dígitos repetidos
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(20)
        );

        ValidationResult result = validationService.validate(dados, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().cpfValido()).isFalse();
        assertThat(result.motivoReprovacao()).contains("sequência de dígitos repetidos");
    }

    @Test
    @DisplayName("Cenário 4: Comprovante de residência com emissão superior a 90 dias deve reprovar")
    void shouldRejectWhenDateIsOlderThan90Days() {
        // Documento com 110 dias atrás (estritamente maior que 90 dias)
        LocalDate dataAntiga = LocalDate.now().minusDays(110);

        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                dataAntiga
        );

        ValidationResult result = validationService.validate(dados, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().emissaoMenor90Dias()).isFalse();
        assertThat(result.motivoReprovacao()).contains("Data de emissão superior a 90 dias: 110 dias atrás");
    }

    @Test
    @DisplayName("Cenário 5: Documento com data futura deve reprovar")
    void shouldRejectWhenDateIsFuture() {
        LocalDate dataFutura = LocalDate.now().plusDays(5);

        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                dataFutura
        );

        ValidationResult result = validationService.validate(dados, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().emissaoMenor90Dias()).isFalse();
        assertThat(result.motivoReprovacao()).containsIgnoringCase("futura");
    }

    @Test
    @DisplayName("Cenário 6: Endereço ausente ou sem numeração identificável deve reprovar")
    void shouldRejectWhenAddressIsMissingOrHasNoNumber() {
        // Endereço sem número
        DadosDocumento dadosSemNumero = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Avenida Paulista", // Sem número
                "São Paulo",
                "SP",
                LocalDate.now().minusDays(10)
        );

        ValidationResult result = validationService.validate(dadosSemNumero, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().enderecoEncontrado()).isFalse();
        assertThat(result.motivoReprovacao()).contains("numeração/logradouro não identificáveis");
    }

    @Test
    @DisplayName("Cenário 7: Documento com texto OCR ilegível (< 30 caracteres) deve reprovar")
    void shouldRejectWhenDocumentIsIllegible() {
        DadosDocumento dados = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(10)
        );

        // Texto com menos de 30 caracteres
        String textoInsuficiente = "Foto borrada";

        ValidationResult result = validationService.validate(dados, textoInsuficiente);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.criterios().documentoLegivel()).isFalse();
        assertThat(result.motivoReprovacao()).contains("Documento ilegível ou com texto OCR insuficiente");
    }

    @Test
    @DisplayName("Cenário 8: Múltiplas falhas simultâneas devem compilar todos os motivos de reprovação")
    void shouldCompileAllRejectionReasonsWhenMultipleRulesFail() {
        DadosDocumento dadosMultiplasFalhas = new DadosDocumento(
                "Joao da Silva",
                "000.000.000-00", // CPF inválido
                "Rua sem nada",   // Endereço sem número
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(120) // Mais de 90 dias
        );

        ValidationResult result = validationService.validate(dadosMultiplasFalhas, TEXTO_BRUTO_LEGIVEL);

        assertThat(result.status()).isEqualTo(StatusValidacao.REPROVADO);
        assertThat(result.falhas()).hasSize(3);
        assertThat(result.motivoReprovacao()).contains("CPF");
        assertThat(result.motivoReprovacao()).contains("Endereço");
        assertThat(result.motivoReprovacao()).contains("superior a 90 dias");
    }
}
