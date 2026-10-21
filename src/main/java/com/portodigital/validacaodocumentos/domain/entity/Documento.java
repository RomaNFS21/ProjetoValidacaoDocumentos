package com.portodigital.validacaodocumentos.domain.entity;

import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome_arquivo_original", nullable = false)
    private String nomeArquivoOriginal;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "texto_bruto_ocr", columnDefinition = "TEXT")
    private String textoBrutoOcr;

    @Column(name = "data_upload", nullable = false, updatable = false)
    private OffsetDateTime dataUpload;

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DadosExtraidos dadosExtraidos;

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Validacao validacao;

    public Documento() {
    }

    public Documento(String nomeArquivoOriginal, String contentType, Long tamanhoBytes, TipoDocumento tipoDocumento) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.tipoDocumento = tipoDocumento;
        this.dataUpload = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dataUpload == null) {
            this.dataUpload = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getTextoBrutoOcr() {
        return textoBrutoOcr;
    }

    public void setTextoBrutoOcr(String textoBrutoOcr) {
        this.textoBrutoOcr = textoBrutoOcr;
    }

    public OffsetDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(OffsetDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    public DadosExtraidos getDadosExtraidos() {
        return dadosExtraidos;
    }

    public void setDadosExtraidos(DadosExtraidos dadosExtraidos) {
        this.dadosExtraidos = dadosExtraidos;
        if (dadosExtraidos != null) {
            dadosExtraidos.setDocumento(this);
        }
    }

    public Validacao getValidacao() {
        return validacao;
    }

    public void setValidacao(Validacao validacao) {
        this.validacao = validacao;
        if (validacao != null) {
            validacao.setDocumento(this);
        }
    }
}
