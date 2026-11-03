package com.portodigital.validacaodocumentos.domain.entity;

import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA raiz (Aggregate Root) que representa o documento submetido ao pipeline de validação.
 */
@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumento tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusValidacao status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TextoBrutoOcr textoBrutoOcr;

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DadosExtraidos dadosExtraidos;

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Validacao validacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = StatusValidacao.EM_PROCESSAMENTO;
        }
    }

    public Documento(String nomeArquivo, TipoDocumento tipoDocumento) {
        this.nomeArquivo = nomeArquivo;
        this.tipoDocumento = tipoDocumento;
        this.status = StatusValidacao.EM_PROCESSAMENTO;
        this.dataCriacao = LocalDateTime.now();
    }

    public Documento(String nomeArquivo, TipoDocumento tipoDocumento, StatusValidacao status) {
        this.nomeArquivo = nomeArquivo;
        this.tipoDocumento = tipoDocumento;
        this.status = status != null ? status : StatusValidacao.EM_PROCESSAMENTO;
        this.dataCriacao = LocalDateTime.now();
    }

    // Getters e Setters explícitos garantindo compatibilidade total
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public StatusValidacao getStatus() {
        return status;
    }

    public void setStatus(StatusValidacao status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public TextoBrutoOcr getTextoBrutoOcr() {
        return textoBrutoOcr;
    }

    public void setTextoBrutoOcr(TextoBrutoOcr textoBrutoOcr) {
        this.textoBrutoOcr = textoBrutoOcr;
        if (textoBrutoOcr != null) {
            textoBrutoOcr.setDocumento(this);
            textoBrutoOcr.setDocumentoId(this.id);
        }
    }

    public DadosExtraidos getDadosExtraidos() {
        return dadosExtraidos;
    }

    public void setDadosExtraidos(DadosExtraidos dadosExtraidos) {
        this.dadosExtraidos = dadosExtraidos;
        if (dadosExtraidos != null) {
            dadosExtraidos.setDocumento(this);
            dadosExtraidos.setDocumentoId(this.id);
        }
    }

    public Validacao getValidacao() {
        return validacao;
    }

    public void setValidacao(Validacao validacao) {
        this.validacao = validacao;
        if (validacao != null) {
            validacao.setDocumento(this);
            validacao.setDocumentoId(this.id);
            if (validacao.getStatus() != null) {
                this.status = validacao.getStatus();
            }
        }
    }

    public String getNomeArquivoOriginal() {
        return this.nomeArquivo;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivo = nomeArquivoOriginal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Documento documento = (Documento) o;
        return Objects.equals(id, documento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
