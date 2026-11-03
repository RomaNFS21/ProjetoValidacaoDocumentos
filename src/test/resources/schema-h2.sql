-- Schema H2 compatível com PostgreSQL para ambiente de testes (@ActiveProfiles("test"))
-- Utilizado quando o Flyway está desabilitado (spring.flyway.enabled=false no application-test.yml)
-- e o Hibernate cria as tabelas via ddl-auto=create-drop a partir das entidades JPA.
--
-- Este arquivo serve apenas como referência de schema. O Hibernate (ddl-auto=create-drop)
-- cria automaticamente as tabelas no H2 a partir das entidades @Entity anotadas.
-- Nenhuma migration manual é necessária no perfil "test".

-- Tabela principal de documentos submetidos para validação
CREATE TABLE IF NOT EXISTS documentos (
    id              UUID          NOT NULL PRIMARY KEY,
    nome_arquivo_original VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    tamanho_bytes   BIGINT        NOT NULL,
    tipo_documento  VARCHAR(50)   NOT NULL,
    texto_bruto_ocr TEXT,
    data_upload     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de dados estruturados extraídos pela IA
CREATE TABLE IF NOT EXISTS dados_extraidos (
    id               UUID         NOT NULL PRIMARY KEY,
    documento_id     UUID         NOT NULL,
    nome_titular     VARCHAR(255),
    cpf              VARCHAR(20),
    endereco_completo VARCHAR(500),
    cidade           VARCHAR(100),
    estado           VARCHAR(10),
    data_emissao     DATE,
    data_extracao    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dados_documento
        FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

-- Tabela de resultados de validação das regras de negócio
CREATE TABLE IF NOT EXISTS validacoes (
    id                UUID        NOT NULL PRIMARY KEY,
    documento_id      UUID        NOT NULL,
    status            VARCHAR(30) NOT NULL,
    motivo_reprovacao TEXT,
    legivel           BOOLEAN     NOT NULL DEFAULT FALSE,
    cpf_valido        BOOLEAN     NOT NULL DEFAULT FALSE,
    endereco_valido   BOOLEAN     NOT NULL DEFAULT FALSE,
    emissao_recente   BOOLEAN     NOT NULL DEFAULT FALSE,
    data_validacao    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_validacao_documento
        FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documentos_data_upload ON documentos(data_upload DESC);
CREATE INDEX IF NOT EXISTS idx_validacoes_status ON validacoes(status);

