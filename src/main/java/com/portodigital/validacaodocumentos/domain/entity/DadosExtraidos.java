package com.portodigital.validacaodocumentos.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA que armazena os dados estruturados extraídos pelo modelo de IA (Spring AI).
 */
@Entity
@Table(name = "dados_extraidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DadosExtraidos {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false, unique = true)
    private Documento documento;

    @Column(name = "documento_id", insertable = false, updatable = false)
    private UUID documentoId;

    @Column(name = "nome", length = 255)
    private String nome;

    @Column(name = "cpf", length = 20)
    private String cpf;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "estado", length = 10)
    private String estado;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "data_extracao", nullable = false, updatable = false)
    private LocalDateTime dataExtracao;

    @PrePersist
    public void prePersist() {
        if (this.dataExtracao == null) {
            this.dataExtracao = LocalDateTime.now();
        }
        if (this.documento != null && this.documentoId == null) {
            this.documentoId = this.documento.getId();
        }
    }

    public DadosExtraidos(Documento documento, String nome, String cpf, String endereco,
                          String cidade, String estado, LocalDate dataEmissao) {
        this.documento = documento;
        this.documentoId = documento != null ? documento.getId() : null;
        this.nome = nome;
        this.cpf = cpf;
        this.endereco = endereco;
        this.cidade = cidade;
        this.estado = estado;
        this.dataEmissao = dataEmissao;
        this.dataExtracao = LocalDateTime.now();
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
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

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }

    public void setDataExtracao(LocalDateTime dataExtracao) {
        this.dataExtracao = dataExtracao;
    }

    public String getNomeTitular() {
        return this.nome;
    }

    public void setNomeTitular(String nomeTitular) {
        this.nome = nomeTitular;
    }

    public String getEnderecoCompleto() {
        return this.endereco;
    }

    public void setEnderecoCompleto(String enderecoCompleto) {
        this.endereco = enderecoCompleto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DadosExtraidos that = (DadosExtraidos) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
