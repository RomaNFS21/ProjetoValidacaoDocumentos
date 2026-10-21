package com.portodigital.validacaodocumentos.domain.entity;

import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "validacoes")
public class Validacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusValidacao status;

    @Column(name = "motivo_reprovacao", columnDefinition = "TEXT")
    private String motivoReprovacao;

    @Column(name = "legivel", nullable = false)
    private boolean legivel;

    @Column(name = "cpf_valido", nullable = false)
    private boolean cpfValido;

    @Column(name = "endereco_valido", nullable = false)
    private boolean enderecoValido;

    @Column(name = "emissao_recente", nullable = false)
    private boolean emissaoRecente;

    @Column(name = "data_validacao", nullable = false, updatable = false)
    private OffsetDateTime dataValidacao;

    public Validacao() {
    }

    public Validacao(Documento documento, StatusValidacao status, String motivoReprovacao,
                     boolean legivel, boolean cpfValido, boolean enderecoValido, boolean emissaoRecente) {
        this.documento = documento;
        this.status = status;
        this.motivoReprovacao = motivoReprovacao;
        this.legivel = legivel;
        this.cpfValido = cpfValido;
        this.enderecoValido = enderecoValido;
        this.emissaoRecente = emissaoRecente;
        this.dataValidacao = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dataValidacao == null) {
            this.dataValidacao = OffsetDateTime.now();
        }
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
    }

    public StatusValidacao getStatus() {
        return status;
    }

    public void setStatus(StatusValidacao status) {
        this.status = status;
    }

    public String getMotivoReprovacao() {
        return motivoReprovacao;
    }

    public void setMotivoReprovacao(String motivoReprovacao) {
        this.motivoReprovacao = motivoReprovacao;
    }

    public boolean isLegivel() {
        return legivel;
    }

    public void setLegivel(boolean legivel) {
        this.legivel = legivel;
    }

    public boolean isCpfValido() {
        return cpfValido;
    }

    public void setCpfValido(boolean cpfValido) {
        this.cpfValido = cpfValido;
    }

    public boolean isEnderecoValido() {
        return enderecoValido;
    }

    public void setEnderecoValido(boolean enderecoValido) {
        this.enderecoValido = enderecoValido;
    }

    public boolean isEmissaoRecente() {
        return emissaoRecente;
    }

    public void setEmissaoRecente(boolean emissaoRecente) {
        this.emissaoRecente = emissaoRecente;
    }

    public OffsetDateTime getDataValidacao() {
        return dataValidacao;
    }

    public void setDataValidacao(OffsetDateTime dataValidacao) {
        this.dataValidacao = dataValidacao;
    }
}
