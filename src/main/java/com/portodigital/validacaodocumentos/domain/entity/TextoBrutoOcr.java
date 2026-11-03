package com.portodigital.validacaodocumentos.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA que representa o texto bruto extraído da imagem pelo motor Tesseract OCR.
 */
@Entity
@Table(name = "texto_bruto_ocr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TextoBrutoOcr {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false, unique = true)
    private Documento documento;

    @Column(name = "documento_id", insertable = false, updatable = false)
    private UUID documentoId;

    @Column(name = "conteudo", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.documento != null && this.documentoId == null) {
            this.documentoId = this.documento.getId();
        }
    }

    public TextoBrutoOcr(Documento documento, String conteudo) {
        this.documento = documento;
        this.documentoId = documento != null ? documento.getId() : null;
        this.conteudo = conteudo;
        this.dataCriacao = LocalDateTime.now();
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

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextoBrutoOcr that = (TextoBrutoOcr) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
