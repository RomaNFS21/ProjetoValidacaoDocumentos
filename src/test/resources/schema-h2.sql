-- Schema H2 compatível com PostgreSQL para ambiente de testes (@ActiveProfiles("test"))

CREATE TABLE IF NOT EXISTS documentos (
    id              UUID          NOT NULL PRIMARY KEY,
    nome_arquivo    VARCHAR(255)  NOT NULL,
    tipo_documento  VARCHAR(50)   NOT NULL,
    status          VARCHAR(30)   NOT NULL,
    data_criacao    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS texto_bruto_ocr (
    id              UUID          NOT NULL PRIMARY KEY,
    documento_id    UUID          NOT NULL UNIQUE,
    conteudo        TEXT,
    data_criacao    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_texto_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dados_extraidos (
    id              UUID          NOT NULL PRIMARY KEY,
    documento_id    UUID          NOT NULL UNIQUE,
    nome            VARCHAR(255),
    cpf             VARCHAR(20),
    endereco        VARCHAR(500),
    cidade          VARCHAR(100),
    estado          VARCHAR(10),
    data_emissao    DATE,
    data_extracao   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dados_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS validacoes (
    id                  UUID        NOT NULL PRIMARY KEY,
    documento_id        UUID        NOT NULL UNIQUE,
    status              VARCHAR(30) NOT NULL,
    mensagem            TEXT,
    cpf_encontrado      BOOLEAN     NOT NULL DEFAULT FALSE,
    cpf_valido          BOOLEAN     NOT NULL DEFAULT FALSE,
    endereco_encontrado BOOLEAN     NOT NULL DEFAULT FALSE,
    documento_legivel   BOOLEAN     NOT NULL DEFAULT FALSE,
    emissao_menor_90_dias BOOLEAN   NOT NULL DEFAULT FALSE,
    data_processamento  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_validacao_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documentos_status ON documentos(status);
CREATE INDEX IF NOT EXISTS idx_documentos_data_criacao ON documentos(data_criacao DESC);
