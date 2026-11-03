package com.portodigital.validacaodocumentos.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Representação embeddable dos critérios de validação avaliados pelo motor de regras.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriosValidacaoEmbeddable {

    @Column(name = "cpf_encontrado", nullable = false)
    @Builder.Default
    private boolean cpfEncontrado = false;

    @Column(name = "cpf_valido", nullable = false)
    @Builder.Default
    private boolean cpfValido = false;

    @Column(name = "endereco_encontrado", nullable = false)
    @Builder.Default
    private boolean enderecoEncontrado = false;

    @Column(name = "documento_legivel", nullable = false)
    @Builder.Default
    private boolean documentoLegivel = false;

    @Column(name = "emissao_menor_90_dias", nullable = false)
    @Builder.Default
    private boolean emissaoMenor90Dias = false;

    public CriteriosValidacaoEmbeddable() {
    }

    public CriteriosValidacaoEmbeddable(boolean cpfEncontrado, boolean cpfValido, boolean enderecoEncontrado,
                                      boolean documentoLegivel, boolean emissaoMenor90Dias) {
        this.cpfEncontrado = cpfEncontrado;
        this.cpfValido = cpfValido;
        this.enderecoEncontrado = enderecoEncontrado;
        this.documentoLegivel = documentoLegivel;
        this.emissaoMenor90Dias = emissaoMenor90Dias;
    }

    public static CriteriosValidacaoEmbeddableBuilder builder() {
        return new CriteriosValidacaoEmbeddableBuilder();
    }

    public static class CriteriosValidacaoEmbeddableBuilder {
        private boolean cpfEncontrado;
        private boolean cpfValido;
        private boolean enderecoEncontrado;
        private boolean documentoLegivel;
        private boolean emissaoMenor90Dias;

        public CriteriosValidacaoEmbeddableBuilder cpfEncontrado(boolean cpfEncontrado) {
            this.cpfEncontrado = cpfEncontrado;
            return this;
        }

        public CriteriosValidacaoEmbeddableBuilder cpfValido(boolean cpfValido) {
            this.cpfValido = cpfValido;
            return this;
        }

        public CriteriosValidacaoEmbeddableBuilder enderecoEncontrado(boolean enderecoEncontrado) {
            this.enderecoEncontrado = enderecoEncontrado;
            return this;
        }

        public CriteriosValidacaoEmbeddableBuilder documentoLegivel(boolean documentoLegivel) {
            this.documentoLegivel = documentoLegivel;
            return this;
        }

        public CriteriosValidacaoEmbeddableBuilder emissaoMenor90Dias(boolean emissaoMenor90Dias) {
            this.emissaoMenor90Dias = emissaoMenor90Dias;
            return this;
        }

        public CriteriosValidacaoEmbeddable build() {
            return new CriteriosValidacaoEmbeddable(cpfEncontrado, cpfValido, enderecoEncontrado, documentoLegivel, emissaoMenor90Dias);
        }
    }

    public boolean isTodosAprovados() {
        return cpfEncontrado && cpfValido && enderecoEncontrado && documentoLegivel && emissaoMenor90Dias;
    }

    public boolean isCpfEncontrado() {
        return cpfEncontrado;
    }

    public void setCpfEncontrado(boolean cpfEncontrado) {
        this.cpfEncontrado = cpfEncontrado;
    }

    public boolean isCpfValido() {
        return cpfValido;
    }

    public void setCpfValido(boolean cpfValido) {
        this.cpfValido = cpfValido;
    }

    public boolean isEnderecoEncontrado() {
        return enderecoEncontrado;
    }

    public void setEnderecoEncontrado(boolean enderecoEncontrado) {
        this.enderecoEncontrado = enderecoEncontrado;
    }

    public boolean isDocumentoLegivel() {
        return documentoLegivel;
    }

    public void setDocumentoLegivel(boolean documentoLegivel) {
        this.documentoLegivel = documentoLegivel;
    }

    public boolean isEmissaoMenor90Dias() {
        return emissaoMenor90Dias;
    }

    public void setEmissaoMenor90Dias(boolean emissaoMenor90Dias) {
        this.emissaoMenor90Dias = emissaoMenor90Dias;
    }
}
