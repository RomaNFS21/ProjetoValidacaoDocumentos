package com.portodigital.validacaodocumentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.service.validation.CpfValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de Estruturação com IA via Spring AI (ChatModel / BeanOutputConverter).
 * Interpreta o texto bruto do OCR e extrai as entidades estruturadas no record {@link DadosDocumento}.
 * Possui mecanismo de extração heurística avançada (Regex + Validação Módulo 11) para contingência
 * resiliente quando a IA externa não estiver configurada ou falhar.
 */
@Service
public class DocumentAiService implements SpringAiService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAiService.class);

    public static final String SYSTEM_PROMPT = """
            Você é um extrator de dados de documentos brasileiros (comprovantes de residência, CNH e RG). Extraia com precisão do texto fornecido os seguintes campos: nome, cpf (apenas números ou formatado), endereco (logradouro, número, complemento e bairro), cidade, estado (sigla com 2 letras) e dataEmissao (formato ISO YYYY-MM-DD). Se não encontrar algum campo, retorne null. Não invente ou alucine informações.
            """;

    private static final Set<String> UFS_BRASIL = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private static final List<String> PALAVRAS_IGNORADAS_NOME = List.of(
            "COMPROVANTE", "RESIDENCIA", "RESIDÊNCIA", "REPUBLICA", "REPÚBLICA",
            "FEDERATIVA", "BRASIL", "MINISTERIO", "MINISTÉRIO", "SECRETARIA",
            "DEFESA", "SEGURANCA", "SEGURANÇA", "INSTITUTO", "IDENTIFICACAO",
            "IDENTIFICAÇÃO", "CARTEIRA", "NACIONAL", "HABILITACAO", "HABILITAÇÃO",
            "DETRAN", "TRANSITO", "TRÂNSITO", "CONSELHO", "ENERGIA", "ELETRICIDADE",
            "COMPANHIA", "DISTRIBUIDORA", "CONCESSIONARIA", "CONCESSIONÁRIA", "AGUA",
            "ÁGUA", "SANEAMENTO", "FATURA", "HISTORICO", "HISTÓRICO", "TELEFONICA",
            "DENATRAN", "SENATRAN", "VALIDADE", "FILIACAO", "FILIAÇÃO", "REGISTRO",
            "GERAL", "ORGAO", "ÓRGÃO", "EMISSOR", "NATURALIDADE", "MUNICIPIO",
            "MUNICÍPIO", "NUMERO", "NÚMERO", "CATEGORIA", "PERMISSAO", "PERMISSÃO",
            "OBSERVACAO", "OBSERVAÇÃO", "CREA", "PORTARIA", "RESOLUCAO", "RESOLUÇÃO"
    );

    // Palavras-rótulo que precedem o nome em documentos de identidade
    private static final List<String> ROTULOS_NOME = List.of(
            "NOME", "NOME DO TITULAR", "NOME DO CLIENTE", "NOME COMPLETO",
            "NOME DO CONDUTOR", "TITULAR", "CLIENTE", "DESTINATÁRIO",
            "DESTINATARIO", "BENEFICIÁRIO", "BENEFICIARIO"
    );

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final BeanOutputConverter<DadosDocumento> outputConverter;
    private final String apiKey;

    @Autowired
    public DocumentAiService(
            @Autowired(required = false) ChatModel chatModel,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.outputConverter = new BeanOutputConverter<>(DadosDocumento.class, objectMapper);
    }

    public DocumentAiService(@Autowired(required = false) ChatModel chatModel, ObjectMapper objectMapper) {
        this(chatModel, objectMapper, "");
    }

    @Override
    public DadosDocumento extrairDados(String textoOcr) {
        return extractDocumentData(textoOcr);
    }

    /**
     * Verifica se a chave configurada é uma chave real válida da OpenAI.
     */
    private boolean isApiKeyValida() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        String trimmed = apiKey.trim().toLowerCase(Locale.ROOT);
        return !trimmed.startsWith("mock")
                && !trimmed.startsWith("test")
                && !trimmed.equals("test-key-not-used")
                && trimmed.length() > 15;
    }

    /**
     * Extrai os dados estruturados a partir do texto bruto retornado pelo Tesseract OCR.
     */
    public DadosDocumento extractDocumentData(String rawOcrText) {
        if (rawOcrText == null || rawOcrText.trim().isEmpty()) {
            log.warn("Texto OCR vazio ou nulo recebido para extração estruturada");
            return new DadosDocumento(null, null, null, null, null, null);
        }

        // Se o ChatModel não estiver configurado ou a chave for nula/mock, usa extração heurística avançada
        if (chatModel == null || !isApiKeyValida()) {
            log.info("Chave da OpenAI não configurada ou fictícia ('{}'). Executando extrator heurístico avançado de alta precisão.",
                    apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 4) + "..." : apiKey);
            return extractFallback(rawOcrText);
        }

        try {
            String formatInstructions = outputConverter.getFormat();
            String completeSystemPrompt = SYSTEM_PROMPT + "\n\nInstruções obrigatórias de formato de saída:\n" + formatInstructions;

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(completeSystemPrompt),
                    new UserMessage("Texto bruto do documento a ser extraído:\n\"\"\"\n" + rawOcrText + "\n\"\"\"")
            ));

            log.info("Enviando requisição de extração estruturada ao Spring AI ChatModel...");
            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("O modelo de IA retornou resposta nula. Aplicando contingência heurística...");
                return extractFallback(rawOcrText);
            }

            String content = response.getResult().getOutput().getContent();
            log.debug("Resposta textual bruta recebida da IA: {}", content);

            String sanitizedJson = sanitizeJson(content);
            DadosDocumento dados = objectMapper.readValue(sanitizedJson, DadosDocumento.class);
            log.info("Dados estruturados com sucesso pela IA: Nome='{}', CPF='{}', Emissão='{}'",
                    dados.nome(), dados.cpf(), dados.dataEmissao());
            return dados;

        } catch (Exception ex) {
            log.warn("Falha na chamada ou desserialização da resposta da OpenAI: {}. Aplicando contingência heurística local...", ex.getMessage());
            return extractFallback(rawOcrText);
        }
    }

    /**
     * Sanitiza a saída da LLM, removendo blocos de código markdown (```json ... ```) e tokens externos.
     */
    public String sanitizeJson(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        String trimmed = rawText.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        trimmed = trimmed.trim();

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    /**
     * Motor de Extração Heurística e Semântica de Alta Resiliência.
     * Analisa o texto linha a linha e por padrões contextuais para capturar Nome, CPF, Data de Emissão,
     * Endereço, Cidade e Estado com alta tolerância a variações de OCR.
     */
    public DadosDocumento extractFallback(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new DadosDocumento(null, null, null, null, null, null);
        }

        String normalizedText = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalizedText.split("\n");

        String cpf = extractCpf(normalizedText);
        LocalDate dataEmissao = extractDataEmissao(lines, normalizedText);
        String nome = extractNome(lines, normalizedText);
        String endereco = extractEndereco(lines, normalizedText);
        String estado = extractEstado(lines, normalizedText);
        String cidade = extractCidade(lines, normalizedText, estado);

        log.info("Extração Heurística Concluída: Nome='{}', CPF='{}', Emissão='{}', Endereço='{}', Cidade='{}', UF='{}'",
                nome, cpf, dataEmissao, endereco, cidade, estado);

        return new DadosDocumento(nome, cpf, endereco, cidade, estado, dataEmissao);
    }

    /**
     * 1. Extração Inteligente de CPF:
     * Busca padrões de CPF e valida os dígitos verificadores (Módulo 11 da Receita Federal).
     * Garante que números de contrato, hidrômetros ou códigos de barras não sejam confundidos com CPF.
     */
    private String extractCpf(String text) {
        List<String> candidatos = new ArrayList<>();

        // Padrão 1: Rotulado (ex: CPF: 123.456.789-00 ou CPF 12345678900)
        Matcher rotulado = Pattern.compile("(?i)(?:CPF|C\\.P\\.F|MF|DOC(?:\\.|UMENTO)?)[\\s.:/=-]*([0-9.\\-\\s]{11,18})").matcher(text);
        while (rotulado.find()) {
            String c = rotulado.group(1).replaceAll("\\D", "");
            if (c.length() == 11) {
                candidatos.add(c);
            }
        }

        // Padrão 2: Formatado tradicional (000.000.000-00 ou 000 000 000 00)
        Matcher formatado = Pattern.compile("\\b(\\d{3})[.\\s](\\d{3})[.\\s](\\d{3})[-.\\s](\\d{2})\\b").matcher(text);
        while (formatado.find()) {
            String c = formatado.group(1) + formatado.group(2) + formatado.group(3) + formatado.group(4);
            candidatos.add(c);
        }

        // Padrão 3: Sequência contínua de 11 dígitos
        Matcher digitos11 = Pattern.compile("\\b(\\d{11})\\b").matcher(text);
        while (digitos11.find()) {
            candidatos.add(digitos11.group(1));
        }

        // Testa cada candidato: Prioriza o que possuir dígitos verificadores válidos pelo algoritmo oficial
        for (String c : candidatos) {
            if (c.chars().distinct().count() > 1 && CpfValidationRule.validarDigitosVerificadores(c)) {
                return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9, 11);
            }
        }

        // Se nenhum passou no módulo 11, retorna o primeiro candidato de 11 dígitos encontrado (para avaliação nas regras)
        if (!candidatos.isEmpty()) {
            String c = candidatos.get(0);
            return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9, 11);
        }

        return null;
    }

    /**
     * 2. Extração de Data de Emissão / Expedição:
     * Prioriza datas próximas a termos de emissão/expedição e descarta datas de nascimento.
     * Suporta padrões específicos de CNH (1ª habilitação, validade) e RG (expedição, emissão).
     */
    private LocalDate extractDataEmissao(String[] lines, String fullText) {
        Pattern datePattern = Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})");

        // Passo 1: Busca em linhas com termos explícitos de emissão/expedição
        // Prioridade alta: emissão, expedição — para CNH e RG
        String[] termosEmissao = {
            "EMISS", "EXPED", "1ª HABILITAÇÃO", "1A HABILITACAO", "1ª HABILITA",
            "PRIMEIRA HABILITACAO", "PRIMEIRA HABILITAÇÃO",
            "DATA DOC", "DATA DE EMISSÃO", "EMITIDO EM", "DT. EM", "DT EM",
            "DATA EMISSÃO", "REGISTRO GERAL"
        };

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String upper = line.toUpperCase(Locale.ROOT);

            // Ignora explicitamente linhas de nascimento ou validade futura
            if (upper.contains("NASC") || upper.contains("ANIVERS") || upper.contains("VALID")) {
                continue;
            }

            boolean contemTermoEmissao = false;
            for (String termo : termosEmissao) {
                if (upper.contains(termo)) {
                    contemTermoEmissao = true;
                    break;
                }
            }

            if (contemTermoEmissao) {
                // Procura na própria linha
                Matcher m = datePattern.matcher(line);
                if (m.find()) {
                    LocalDate d = parseLocalDate(m.group(1), m.group(2), m.group(3));
                    if (d != null && !d.isAfter(LocalDate.now())) return d;
                }
                // Procura nas 2 linhas seguintes (formato label-valor do RG/CNH)
                for (int j = i + 1; j <= Math.min(i + 2, lines.length - 1); j++) {
                    Matcher mNext = datePattern.matcher(lines[j]);
                    if (mNext.find()) {
                        LocalDate d = parseLocalDate(mNext.group(1), mNext.group(2), mNext.group(3));
                        if (d != null && !d.isAfter(LocalDate.now())) return d;
                    }
                }
            }
        }

        // Passo 2: Coleta todas as datas do documento excluindo as de nascimento e futuras
        List<LocalDate> datasCandidatas = new ArrayList<>();
        // Encontra posições de linhas com "NASC" para marcar datas nessas regiões como inválidas
        Set<Integer> posNascimento = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String u = lines[i].toUpperCase(Locale.ROOT);
            if (u.contains("NASC") || u.contains("ANIVERS")) {
                posNascimento.add(i);
                if (i + 1 < lines.length) posNascimento.add(i + 1);
            }
        }

        for (int i = 0; i < lines.length; i++) {
            if (posNascimento.contains(i)) continue; // Pula linhas de nascimento

            String u = lines[i].toUpperCase(Locale.ROOT);
            if (u.contains("VALID")) continue; // Pula linhas de validade (data futura)

            Matcher m = datePattern.matcher(lines[i]);
            while (m.find()) {
                LocalDate d = parseLocalDate(m.group(1), m.group(2), m.group(3));
                if (d != null && !d.isAfter(LocalDate.now()) && !d.isBefore(LocalDate.now().minusYears(15))) {
                    datasCandidatas.add(d);
                }
            }
        }

        // Retorna a data mais recente (provável data de emissão)
        if (!datasCandidatas.isEmpty()) {
            datasCandidatas.sort(Collections.reverseOrder());
            return datasCandidatas.get(0);
        }

        return null;
    }

    private LocalDate parseLocalDate(String diaStr, String mesStr, String anoStr) {
        try {
            int dia = Integer.parseInt(diaStr);
            int mes = Integer.parseInt(mesStr);
            int ano = Integer.parseInt(anoStr);
            if (dia >= 1 && dia <= 31 && mes >= 1 && mes <= 12 && ano >= 1950 && ano <= 2100) {
                return LocalDate.of(ano, mes, dia);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 3. Extração Inteligente de Nome:
     * Trata formatos na mesma linha (ex: "NOME DO CLIENTE: JOÃO DA SILVA")
     * e formatos em linhas consecutivas (ex: Linha 1 "NOME", Linha 2 "JOÃO DA SILVA" em CNH/RG).
     */
    private String extractNome(String[] lines, String fullText) {
        // Padrão geral de rótulo de nome (mesma linha ou linha seguinte)
        Pattern labelPattern = Pattern.compile("(?i)^(?:NOME(?:\\s+(?:DO\\s+)?(?:TITULAR|CLIENTE|COMPLETO|CONDUTOR|CIVIL))?|TITULAR|CLIENTE|DESTINAT[AÁ]RIO|BENEFICI[AÁ]RIO)[\\s.:=-]*(.*)$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher m = labelPattern.matcher(line);

            if (m.matches()) {
                String restoLinha = m.group(1).trim();

                // Caso 1: Nome preenchido na mesma linha após o rótulo
                if (isNomePessoaValido(restoLinha)) {
                    return limparNome(restoLinha);
                }

                // Caso 2: Rótulo sozinho na linha (comum em CNH e RG), nome na próxima(s) linha(s)
                for (int j = i + 1; j <= Math.min(i + 3, lines.length - 1); j++) {
                    String prox = lines[j].trim();
                    if (!prox.isEmpty() && isNomePessoaValido(prox)) {
                        return limparNome(prox);
                    }
                    // Para se encontrar outro rótulo
                    if (prox.toUpperCase(Locale.ROOT).matches("(?:CPF|RG|FILIA[ÇC][AÃ]O|DATA|DOC|NASC|CATEG|VALID).*")) {
                        break;
                    }
                }
            }
        }

        // Padrão CNH / RG com delimitadores específicos (texto entre NOME/CONDUTOR e próximo campo)
        Pattern cnhNomePattern = Pattern.compile("(?i)(?:NOME|CONDUTOR)[\\s\\n:.-]+([A-Za-zÀ-ÖØ-öø-ÿ\\s]{5,55})[\\s\\n]+(?:DOC|IDENTIDADE|FILIA|CPF|NASC|DATA|RG|CATEG|VALID|ASSINA)");
        Matcher cnhMatcher = cnhNomePattern.matcher(fullText);
        while (cnhMatcher.find()) {
            String candidato = cnhMatcher.group(1).trim();
            if (isNomePessoaValido(candidato)) {
                return limparNome(candidato);
            }
        }

        // Fallback: Busca por qualquer linha em caixa alta com 2+ palavras antes de um campo CPF/RG
        Pattern anteCpf = Pattern.compile("(?m)^([A-ZÁÉÍÓÚÂÊÎÔÛÃÕÀÈÌÒÙÇ][A-ZÁÉÍÓÚÂÊÎÔÛÃÕÀÈÌÒÙÇ\\s]{4,45})\\n(?:CPF|RG|DOC|FILIA|NASC)");
        Matcher mAnte = anteCpf.matcher(fullText);
        while (mAnte.find()) {
            String cand = mAnte.group(1).trim();
            if (isNomePessoaValido(cand)) return limparNome(cand);
        }

        return null;
    }

    private boolean isNomePessoaValido(String candidate) {
        if (candidate == null || candidate.trim().length() < 4) return false;

        String upper = candidate.trim().toUpperCase(Locale.ROOT);

        // Rejeita se contiver palavras proibidas (nomes de órgãos/documentos)
        for (String palavraProibida : PALAVRAS_IGNORADAS_NOME) {
            if (upper.contains(palavraProibida)) return false;
        }

        // Rejeita se for um rótulo conhecido
        for (String rotulo : ROTULOS_NOME) {
            if (upper.equals(rotulo)) return false;
        }

        // Deve conter ao menos 2 palavras com letras (Nome e Sobrenome)
        String[] partes = candidate.trim().split("\\s+");
        if (partes.length < 2) return false;

        // Cada parte deve começar com letra
        for (String parte : partes) {
            if (!parte.isEmpty() && !Character.isLetter(parte.charAt(0))) return false;
        }

        // Não deve conter muitos dígitos
        long digitos = candidate.chars().filter(Character::isDigit).count();
        return digitos == 0;
    }

    private String limparNome(String nome) {
        if (nome == null) return null;
        // Remove eventuais prefixos residuais de rótulo
        String limpo = nome.replaceAll("(?i)^(?:DO CLIENTE|DO TITULAR|DO CONDUTOR|COMPLETO|CIVIL)[\\s.:=-]*", "").trim();
        // Remove caracteres especiais de pontuação no início ou fim
        limpo = limpo.replaceAll("^[^a-zA-ZÀ-ÿ]+", "").replaceAll("[^a-zA-ZÀ-ÿ]+$", "").trim();
        // Capitaliza corretamente (ex: "VICTOR ROMA" -> "Victor Roma")
        if (limpo.equals(limpo.toUpperCase(Locale.ROOT))) {
            StringBuilder sb = new StringBuilder();
            for (String part : limpo.split("\\s+")) {
                if (!part.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    // Preposições e artigos em minúsculo
                    if (part.matches("(?i)DE|DA|DO|DAS|DOS|E|EM")) {
                        sb.append(part.toLowerCase(Locale.ROOT));
                    } else {
                        sb.append(Character.toUpperCase(part.charAt(0)));
                        sb.append(part.substring(1).toLowerCase(Locale.ROOT));
                    }
                }
            }
            return sb.toString();
        }
        return limpo;
    }

    /**
     * 4. Extração de Endereço Residencial:
     * Identifica logradouros brasileiros típicos (Rua, Av, Travessa) e números prediais.
     */
    private String extractEndereco(String[] lines, String fullText) {
        Pattern prefixoRua = Pattern.compile("(?i)\\b(Rua|R\\.|Avenida|Av\\.?|Alameda|Al\\.?|Travessa|Trav\\.?|Rodovia|Rod\\.?|Estrada|Estr\\.?|Pra[cç]a|Largo|Quadra|Lote)\\b\\s+([^\\n,]{3,50})[,\\s]+(N[º°]?\\s*\\d+|\\d+|S/?N)\\b", Pattern.CASE_INSENSITIVE);

        // Busca prioritária nas linhas
        for (String line : lines) {
            String trimmed = line.trim();
            Matcher m = prefixoRua.matcher(trimmed);
            if (m.find()) {
                return trimmed;
            }
        }

        // Busca por rótulo "Endereço: ..."
        Pattern labeledEnd = Pattern.compile("(?i)(?:Endere[cç]o|Logradouro|Local de Consumo|Instala[cç][aã]o)[\\s.:=-]+([^\\n]{8,90})");
        Matcher mEnd = labeledEnd.matcher(fullText);
        if (mEnd.find()) {
            String candidato = mEnd.group(1).trim();
            // Verifica se possui dígitos para numeração
            if (candidato.matches(".*\\d+.*")) {
                return candidato;
            }
        }

        return null;
    }

    /**
     * 5. Extração de Estado (UF):
     * Detecta sigla do estado em padrões "Cidade - UF" ou isolada.
     */
    private String extractEstado(String[] lines, String fullText) {
        Pattern padraoUf = Pattern.compile("(?i)[-,/\\s]\\s*([A-Z]{2})\\b");
        for (String line : lines) {
            Matcher m = padraoUf.matcher(line);
            while (m.find()) {
                String sigla = m.group(1).toUpperCase(Locale.ROOT);
                if (UFS_BRASIL.contains(sigla)) {
                    return sigla;
                }
            }
        }
        return null;
    }

    /**
     * 6. Extração de Cidade:
     * Detecta o município associado ao estado ou rotulado explicitamente.
     */
    private String extractCidade(String[] lines, String fullText, String estado) {
        if (estado != null) {
            Pattern cidadeEstadoPattern = Pattern.compile("(?i)([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,30})\\s*[-/]\\s*" + estado);
            Matcher m = cidadeEstadoPattern.matcher(fullText);
            if (m.find()) {
                String c = m.group(1).trim();
                if (!c.equalsIgnoreCase("Bairro") && !c.equalsIgnoreCase("Rua")) {
                    return c;
                }
            }
        }

        Pattern rotuloCidade = Pattern.compile("(?i)(?:Cidade|Munic[ií]pio)[\\s.:=-]+([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,30})");
        Matcher mRot = rotuloCidade.matcher(fullText);
        if (mRot.find()) {
            return mRot.group(1).trim();
        }

        return null;
    }
}
