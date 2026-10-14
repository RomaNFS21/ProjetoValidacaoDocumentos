# 📄 Validação Automática de Documentos

Uma solução inteligente e automatizada para recepção, pré-processamento, leitura via OCR, estruturação com Inteligência Artificial e validação de regras de negócio em documentos (como comprovantes de residência e documentos de identificação).

---

## 📌 Sumário
- [Sobre o Projeto](#-sobre-o-projeto)
- [Problema e Solução](#-problema-e-solução)
- [Público-Alvo e Casos de Uso](#-público-alvo-e-casos-de-uso)
- [Arquitetura e Fluxo do Sistema](#-arquitetura-e-fluxo-do-sistema)
- [Dados Extraídos e Regras de Negócio](#-dados-extraídos-e-regras-de-negócio)
- [Exemplo Prático de Transformação](#-exemplo-prático-de-transformação)
- [Escopo do MVP](#-escopo-do-mvp)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)

---

## 💡 Sobre o Projeto

A **Validação Automática de Documentos** visa eliminar processos manuais, lentos e suscetíveis a erros humanos durante o cadastramento e análise documental. A aplicação combina visão computacional (**OpenCV**), reconhecimento óptico de caracteres (**Tesseract OCR**), modelos de linguagem (**IA / LLMs**) e backend robusto (**Java + Spring Boot**) para validar comprovantes com precisão e velocidade.

---

## 🎯 Problema e Solução

* **O Problema:** Ineficiência, lentidão e alta taxa de erros manuais na conferência de documentos recebidos em processos de onboarding, cadastro de novos clientes ou análise de crédito/admissão.
* **A Solução:** Um pipeline de processamento em 6 etapas que recebe o arquivo (imagem ou PDF), otimiza a qualidade da imagem, extrai o texto bruto, estrutura os dados em formato JSON usando IA, aplica regras estritas de validação em Java e persiste os resultados em banco PostgreSQL.

---

## 👥 Público-Alvo e Casos de Uso

* **Setor de Backoffice e Operações:** Automação e otimização da análise documental do dia a dia.
* **Analistas de Cadastro / Compliance:** Verificação automatizada de regras regulatórias e de negócio.
* **Equipes de RH e Financeiro:** Validação rápida de comprovantes para processos admissionais ou liberação de pagamentos e crédito.

---

## 🏗 Arquitetura e Fluxo do Sistema

O fluxo do sistema é dividido em **6 etapas interconectadas**:

```
 ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 │ 1. Documento│ ──> │  2. OpenCV  │ ──> │3. Tesseract │
 └─────────────┘     └─────────────┘     └─────────────┘
                                                │
 ┌─────────────┐     ┌─────────────┐            ▼
 │ 6. Resultado│ <── │ 5. Java     │ <── ┌─────────────┐
 │& Persistência│    │(Spring Boot)│     │ 4. IA (LLM) │
 └─────────────┘     └─────────────┘     └─────────────┘
```

| Etapa | Componente | Responsabilidade |
| :--- | :--- | :--- |
| **1. Envio** | **Documento** | Upload do arquivo pelo usuário nos formatos PNG, JPEG ou PDF. |
| **2. Tratamento** | **OpenCV** | Remoção de ruído, correção de rotação, ajuste de contraste e binarização (preto e branco). |
| **3. Extração** | **Tesseract OCR** | Leitura dos caracteres da imagem tratada e conversão para texto bruto (`String`). |
| **4. Estruturação**| **IA / LLM** | Interpretação do texto bruto e mapeamento estruturado em formato **JSON** via Spring AI. |
| **5. Validação** | **Java (Spring Boot)** | Deserialização para `record` Java, validação dos campos obrigatórios e checagem da temporalidade. |
| **6. Persistência**| **PostgreSQL & API** | Armazenamento do status e dos dados no banco e retorno do resultado via API REST. |

---

## 📊 Dados Extraídos e Regras de Negócio

### Campos Extraídos

| Campo | Tipo | Descrição / Formato |
| :--- | :--- | :--- |
| `nome` | `String` | Nome completo do titular do documento |
| `cpf` | `String` | CPF no formato `000.000.000-00` ou apenas números |
| `endereco` | `String` | Logradouro, número, complemento e bairro |
| `cidade` | `String` | Nome do município informado |
| `estado` | `String` | Sigla do Estado (ex: PE, SP, BA) |
| `dataEmissao` | `LocalDate` | Data de geração da fatura/documento (`YYYY-MM-DD`) |

### Critérios de Decisão

```
                               ┌───────────────────────────┐
                               │ Documento sob Validação   │
                               └─────────────┬─────────────┘
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
            [ Cumpre todos os critérios ]               [ Falha em pelo menos 1 critério ]
                       │                                           │
                       ▼                                           ▼
             🟢 STATUS: APROVADO                         🔴 STATUS: REPROVADO
   • CPF localizado e válido                   • CPF ausente ou inválido
   • Endereço completo identificado            • Endereço incompleto/ausente
   • Texto legível após OCR                    • Imagem de baixa qualidade/ilegível
   • Emissão < 90 dias                         • Emissão >= 90 dias
```

---

## 🔄 Exemplo Prático de Transformação

### 1. Texto Bruto (Saída do Tesseract)
```text
COMPROVANTE DE RESIDÊNCIA
Nome: João da Silva
CPF: 123.456.789-00
Endereço: Rua das Flores, 150
Guanambi - BA
Data de emissão: 10/08/2026
```

### 2. Prompt e JSON Estruturado (Saída da IA)
**Prompt enviado à LLM:**
> "Extraia as informações do documento abaixo. Retorne apenas os campos: nome, cpf, endereco, cidade, estado e dataEmissao. Se não encontrar um campo, retorne null. Não invente informações."

**Resposta em JSON:**
```json
{
  "nome": "João da Silva",
  "cpf": "12345678900",
  "endereco": "Rua das Flores, 150",
  "cidade": "Guanambi",
  "estado": "BA",
  "dataEmissao": "2026-08-10"
}
```

### 3. Objeto Java (`Record`)
```java
public record DadosDocumento(
    String nome,
    String cpf,
    String endereco,
    String cidade,
    String estado,
    LocalDate dataEmissao
) {}
```

---

## 🚀 Escopo do MVP (Minimum Viable Product)

A primeira entrega funcional contempla:
- [x] **API REST** construída com Java e Spring Boot.
- [x] **Pipeline de Imagem e OCR:** Suporte a 1 tipo específico de documento (**Comprovante de Residência**).
- [x] **Integração Spring AI:** Conectado à API do **OpenAI** ou **Google Gemini**.
- [x] **Motor de Regras:** Validação de CPF, Endereço e cálculo de recência temporal (< 90 dias).
- [x] **Persistência:** Banco de dados PostgreSQL gravando o registro do documento e o status final (`APROVADO` ou `REPROVADO`).

---

## 🛠 Tecnologias Utilizadas

* **Linguagem & Framework:** Java 17+, Spring Boot 3.x
* **Visão Computacional & OCR:** OpenCV, Tesseract OCR
* **Integração com IA:** Spring AI (OpenAI API / Google Gemini API)
* **Banco de Dados:** PostgreSQL
* **Gerenciamento de Dependências:** Maven / Gradle

---

## 🧰 Alternativas e Evoluções Futuras

* **Extração por Regex:** Para layouts fixos e simples, é possível utilizar Expressões Regulares (`Pattern` / `Matcher` em Java) como alternativa sem custos de chamadas a LLMs.
* **IA Multimodal:** Avaliação direta da imagem usando modelos de visão multimodal (ex: Gemini Vision, GPT-4o), reduzindo o pipeline ao eliminar a necessidade do Tesseract OCR.
