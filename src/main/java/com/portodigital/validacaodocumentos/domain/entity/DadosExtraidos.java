package com.portodigital.validacaodocumentos.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dados_extraidos")
public class DadosExtraidos {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "nome_titular")
    private String nomeTitular;

    @Column(name = "cpf", length = 20)
    private String cpf;

    @Column(name = "endereco_completo", length = 500)
    private String enderecoCompleto;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "estado", length = 10)
    private String estado;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "data_extracao", nullable = false, updatable = false)
    private OffsetDateTime dataExtracao;

    public DadosExtraidos() {
    }

    public DadosExtraidos(Documento documento, String nomeTitular, String cpf, String enderecoCompleto,
                          String cidade, String estado, LocalDate dataEmissao) {
        this.documento = documento;
        this.nomeTitular = nomeTitular;
        this.cpf = cpf;
        this.enderecoCompleto = enderecoCompleto;
        this.cidade = cidade;
        this.estado = estado;
        this.dataEmissao = dataEmissao;
        this.dataExtracao = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dataExtracao == null) {
            this.dataExtracao = OffsetDateTime.now();
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

    public String getNomeTitular() {
        return nomeTitular;
    }

    public void setNomeTitular(String nomeTitular) {
        this.nomeTitular = nomeTitular;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEnderecoCompleto() {
        return enderecoCompleto;
    }

    public void setEnderecoCompleto(String enderecoCompleto) {
        this.enderecoCompleto = enderecoCompleto;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public OffsetDateTime getDataExtracao() {
        return dataExtracao;
    }

    public void setDataExtracao(OffsetDateTime dataExtracao) {
        this.dataExtracao = dataExtracao;
    }
}
