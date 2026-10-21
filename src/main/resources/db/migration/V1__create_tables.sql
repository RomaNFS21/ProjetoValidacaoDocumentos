-- V1__create_tables.sql
-- Tabelas do sistema de validação automática de documentos

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Tabela de Documentos enviados
CREATE TABLE IF NOT EXISTS documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_arquivo_original VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    texto_bruto_ocr TEXT,
    data_upload TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Tabela de Dados Estruturados Extraídos pela IA
CREATE TABLE IF NOT EXISTS dados_extraidos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    nome_titular VARCHAR(255),
    cpf VARCHAR(20),
    endereco_completo VARCHAR(500),
    cidade VARCHAR(100),
    estado VARCHAR(10),
    data_emissao DATE,
    data_extracao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_dados_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

-- 3. Tabela de Resultados de Validação das Regras de Negócio
CREATE TABLE IF NOT EXISTS validacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    motivo_reprovacao TEXT,
    legivel BOOLEAN NOT NULL DEFAULT FALSE,
    cpf_valido BOOLEAN NOT NULL DEFAULT FALSE,
    endereco_valido BOOLEAN NOT NULL DEFAULT FALSE,
    emissao_recente BOOLEAN NOT NULL DEFAULT FALSE,
    data_validacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_validacao_documento FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documentos_data_upload ON documentos(data_upload DESC);
CREATE INDEX IF NOT EXISTS idx_validacoes_status ON validacoes(status);
