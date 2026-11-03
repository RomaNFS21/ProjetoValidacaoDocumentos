-- V1__create_tables.sql
-- Camada de Persistência Completa para Validação Automática de Documentos

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Tabela de Documentos enviados
CREATE TABLE IF NOT EXISTS documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_arquivo VARCHAR(255) NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_criacao TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Tabela de Texto Bruto Extraído via OCR (Tesseract)
CREATE TABLE IF NOT EXISTS texto_bruto_ocr (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL UNIQUE,
    conteudo TEXT,
    data_criacao TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_texto_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

-- 3. Tabela de Dados Estruturados Extraídos pela IA (Spring AI)
CREATE TABLE IF NOT EXISTS dados_extraidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL UNIQUE,
    nome VARCHAR(255),
    cpf VARCHAR(20),
    endereco VARCHAR(500),
    cidade VARCHAR(100),
    estado VARCHAR(10),
    data_emissao DATE,
    data_extracao TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_dados_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

-- 4. Tabela de Resultados de Validação das Regras de Negócio
CREATE TABLE IF NOT EXISTS validacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    mensagem TEXT,
    cpf_encontrado BOOLEAN NOT NULL DEFAULT FALSE,
    cpf_valido BOOLEAN NOT NULL DEFAULT FALSE,
    endereco_encontrado BOOLEAN NOT NULL DEFAULT FALSE,
    documento_legivel BOOLEAN NOT NULL DEFAULT FALSE,
    emissao_menor_90_dias BOOLEAN NOT NULL DEFAULT FALSE,
    data_processamento TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_validacao_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

-- Índices de performance para busca e filtros frequentes
CREATE INDEX IF NOT EXISTS idx_documentos_status ON documentos(status);
CREATE INDEX IF NOT EXISTS idx_documentos_data_criacao ON documentos(data_criacao DESC);
CREATE INDEX IF NOT EXISTS idx_texto_documento_id ON texto_bruto_ocr(documento_id);
CREATE INDEX IF NOT EXISTS idx_dados_documento_id ON dados_extraidos(documento_id);
CREATE INDEX IF NOT EXISTS idx_validacoes_documento_id ON validacoes(documento_id);
