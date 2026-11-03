package com.portodigital.validacaodocumentos.domain.entity;

import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA que armazena o resultado final do motor de validação de regras de negócio.
 */
@Entity
@Table(name = "validacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Validacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false, unique = true)
    private Documento documento;

    @Column(name = "documento_id", insertable = false, updatable = false)
    private UUID documentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusValidacao status;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Embedded
    @Builder.Default
    private CriteriosValidacaoEmbeddable criterios = new CriteriosValidacaoEmbeddable();

    @Column(name = "data_processamento", nullable = false, updatable = false)
    private LocalDateTime dataProcessamento;

    @PrePersist
    public void prePersist() {
        if (this.dataProcessamento == null) {
            this.dataProcessamento = LocalDateTime.now();
        }
        if (this.documento != null && this.documentoId == null) {
            this.documentoId = this.documento.getId();
        }
    }

    public Validacao(Documento documento, StatusValidacao status, String mensagem, CriteriosValidacaoEmbeddable criterios) {
        this.documento = documento;
        this.documentoId = documento != null ? documento.getId() : null;
        this.status = status;
        this.mensagem = mensagem;
        this.criterios = criterios != null ? criterios : new CriteriosValidacaoEmbeddable();
        this.dataProcessamento = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Documento getDocumento() {
        return documento;
    }

    public void setDocumento(Documento documento) {
        this.documento = documento;
        if (documento != null) {
            this.documentoId = documento.getId();
        }
    }

    public UUID getDocumentoId() {
        return documentoId != null ? documentoId : (documento != null ? documento.getId() : null);
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public StatusValidacao getStatus() {
        return status;
    }

    public void setStatus(StatusValidacao status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public CriteriosValidacaoEmbeddable getCriterios() {
        return criterios;
    }

    public void setCriterios(CriteriosValidacaoEmbeddable criterios) {
        this.criterios = criterios;
    }

    public LocalDateTime getDataProcessamento() {
        return dataProcessamento;
    }

    public void setDataProcessamento(LocalDateTime dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
    }

    public String getMotivoReprovacao() {
        return this.mensagem;
    }

    public void setMotivoReprovacao(String motivoReprovacao) {
        this.mensagem = motivoReprovacao;
    }

    public boolean isLegivel() {
        return this.criterios != null && this.criterios.isDocumentoLegivel();
    }

    public boolean isCpfValido() {
        return this.criterios != null && this.criterios.isCpfValido();
    }

    public boolean isEnderecoValido() {
        return this.criterios != null && this.criterios.isEnderecoEncontrado();
    }

    public boolean isEmissaoRecente() {
        return this.criterios != null && this.criterios.isEmissaoMenor90Dias();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Validacao validacao = (Validacao) o;
        return Objects.equals(id, validacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
