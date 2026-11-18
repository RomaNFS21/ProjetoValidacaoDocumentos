package com.portodigital.validacaodocumentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de Estruturação com IA via Spring AI (ChatModel /
 * BeanOutputConverter).
 * Interpreta o texto bruto do OCR e extrai as entidades estruturadas no record
 * {@link DadosDocumento}.
 * Possui mecanismo de fallback resiliente caso o modelo de IA ou chave não
 * estejam disponíveis.
 */
@Service
public class DocumentAiService implements SpringAiService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAiService.class);

    public static final String SYSTEM_PROMPT = """
            Você é um extrator de dados de documentos brasileiros (comprovantes de residência, CNH e RG). Extraia com precisão do texto fornecido os seguintes campos: nome, cpf (apenas números ou formatado), endereco (logradouro, número, complemento e bairro), cidade, estado (sigla com 2 letras) e dataEmissao (formato ISO YYYY-MM-DD). Se não encontrar algum campo, retorne null. Não invente ou alucine informações.
            """;

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
     * Verifica se a chave configurada é uma chave válida da OpenAI.
     */
    private boolean isApiKeyValida() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        String trimmed = apiKey.trim().toLowerCase();
        return !trimmed.startsWith("mock")
                && !trimmed.startsWith("test")
                && !trimmed.equals("test-key-not-used")
                && trimmed.length() > 15;
    }

    /**
     * Extrai os dados estruturados a partir do texto bruto retornado pelo Tesseract
     * OCR.
     *
     * @param rawOcrText Texto bruto do documento
     * @return {@link DadosDocumento} estruturado
     */
    public DadosDocumento extractDocumentData(String rawOcrText) {
        if (rawOcrText == null || rawOcrText.trim().isEmpty()) {
            log.warn("Texto OCR vazio ou nulo recebido para extração estruturada");
            return new DadosDocumento(null, null, null, null, null, null);
        }

        // Se o ChatModel não estiver configurado ou a chave for nula/mock, usa extração
        // local inteligente
        if (chatModel == null || !isApiKeyValida()) {
            log.info(
                    "Chave da OpenAI não configurada ou fictícia ('{}'). Utilizando extrator local baseado em OCR e expressões regulares.",
                    apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 4) + "..." : apiKey);
            return extractFallback(rawOcrText);
        }

        try {
            String formatInstructions = outputConverter.getFormat();
            String completeSystemPrompt = SYSTEM_PROMPT + "\n\nInstruções obrigatórias de formato de saída:\n"
                    + formatInstructions;

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(completeSystemPrompt),
                    new UserMessage("Texto bruto do documento a ser extraído:\n\"\"\"\n" + rawOcrText + "\n\"\"\"")));

            log.info("Enviando requisição de extração estruturada ao Spring AI ChatModel...");
            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("O modelo de IA retornou uma resposta nula. Aplicando contingência heurística...");
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
            log.warn(
                    "Falha na chamada ou desserialização da resposta da OpenAI: {}. Aplicando contingência heurística local...",
                    ex.getMessage());
            DadosDocumento fallback = extractFallback(rawOcrText);
            if (fallback != null) {
                log.info("Contingência heurística local aplicada com sucesso");
                return fallback;
            }
            return new DadosDocumento(null, null, null, null, null, null);
        }
    }

    /**
     * Sanitiza a saída da LLM, removendo blocos de código markdown (```json ...
     * ```) e tokens externos.
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
     * Fallback heurístico resiliente via Expressões Regulares para contingência em
     * falhas de API externa ou execução local.
     */
    public DadosDocumento extractFallback(String text) {
        if (text == null) {
            return new DadosDocumento(null, null, null, null, null, null);
        }

        // 1. CPF (formatado 000.000.000-00 ou 11 dígitos ou rotulado)
        String cpf = null;
        Matcher cpfPrefixedMatcher = Pattern.compile("(?i)(?:CPF|C\\.P\\.F|MF)[\\s.:=-]*([0-9.\\-]{11,14})")
                .matcher(text);
        if (cpfPrefixedMatcher.find()) {
            cpf = cpfPrefixedMatcher.group(1).trim();
        } else {
            Matcher cpfFormattedMatcher = Pattern.compile("\\b(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})\\b").matcher(text);
            if (cpfFormattedMatcher.find()) {
                cpf = cpfFormattedMatcher.group(1).trim();
            } else {
                Matcher cpfDigitsMatcher = Pattern.compile("\\b(\\d{11})\\b").matcher(text);
                if (cpfDigitsMatcher.find()) {
                    String d = cpfDigitsMatcher.group(1);
                    cpf = d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-"
                            + d.substring(9, 11);
                }
            }
        }

        // 2. Data de Emissão / Expedição
        LocalDate dataEmissao = null;
        Matcher datePrefixedMatcher = Pattern.compile(
                "(?i)(?:Emiss[aã]o|Data|Expedi[cç][aã]o|Vencimento|Data\\s*Doc)[\\s.:=-]*(\\d{2})[/.-](\\d{2})[/.-](\\d{4})")
                .matcher(text);
        if (datePrefixedMatcher.find()) {
            try {
                String d = datePrefixedMatcher.group(1);
                String m = datePrefixedMatcher.group(2);
                String y = datePrefixedMatcher.group(3);
                dataEmissao = LocalDate.parse(y + "-" + m + "-" + d);
            } catch (Exception ignored) {
            }
        }

        if (dataEmissao == null) {
            Matcher dateMatcher = Pattern.compile("\\b(\\d{2})[/.-](\\d{2})[/.-](\\d{4})\\b").matcher(text);
            if (dateMatcher.find()) {
                try {
                    String d = dateMatcher.group(1);
                    String m = dateMatcher.group(2);
                    String y = dateMatcher.group(3);
                    dataEmissao = LocalDate.parse(y + "-" + m + "-" + d);
                } catch (Exception ignored) {
                }
            }
        }

        if (dataEmissao == null) {
            Matcher isoMatcher = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b").matcher(text);
            if (isoMatcher.find()) {
                try {
                    dataEmissao = LocalDate.parse(isoMatcher.group(0));
                } catch (Exception ignored) {
                }
            }
        }

        // 3. Nome
        String nome = null;
        Matcher nomeMatcher = Pattern
                .compile("(?i)(?:Nome|Titular|Destinat[aá]rio|Condutor)[\\s.:=-]*([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,45})")
                .matcher(text);
        if (nomeMatcher.find()) {
            nome = nomeMatcher.group(1).trim();
            // Limpa quebras de linha se houver
            if (nome.contains("\n")) {
                nome = nome.split("\n")[0].trim();
            }
        }

        // 4. Endereço
        String endereco = null;
        Matcher endMatcher = Pattern.compile(
                "(?i)(?:Endere[cç]o|Rua|Av\\.?|Avenida|Travessa|Alameda|Rodovia)[\\s.:=-]*([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s,.-]{5,85})")
                .matcher(text);
        if (endMatcher.find()) {
            endereco = endMatcher.group(1).trim();
            if (endereco.contains("\n")) {
                endereco = endereco.split("\n")[0].trim();
            }
        }

        // 5. Estado (UF)
        String estado = null;
        Matcher ufMatcher = Pattern.compile("\\b([A-Z]{2})\\b").matcher(text);
        while (ufMatcher.find()) {
            String candidate = ufMatcher.group(1);
            if ("AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO"
                    .contains(candidate)) {
                estado = candidate;
                break;
            }
        }

        // 6. Cidade
        String cidade = null;
        Matcher cidadeMatcher = Pattern.compile("(?i)(?:Cidade|Munic[ií]pio)[\\s.:=-]*([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,35})")
                .matcher(text);
        if (cidadeMatcher.find()) {
            cidade = cidadeMatcher.group(1).trim();
            if (cidade.contains("\n")) {
                cidade = cidade.split("\n")[0].trim();
            }
        }

        return new DadosDocumento(nome, cpf, endereco, cidade, estado, dataEmissao);
    }
}
