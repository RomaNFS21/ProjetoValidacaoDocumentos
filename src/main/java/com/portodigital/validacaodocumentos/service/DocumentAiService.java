package com.portodigital.validacaodocumentos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.exception.AiProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de Estruturação com IA via Spring AI (ChatModel / BeanOutputConverter).
 * Interpreta o texto bruto do OCR e extrai as entidades estruturadas no record {@link DadosDocumento}.
 */
@Service
public class DocumentAiService implements SpringAiService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAiService.class);

    public static final String SYSTEM_PROMPT = """
            Você é um extrator de dados de comprovantes de residência brasileiros. Extraia com precisão do texto fornecido os seguintes campos: nome, cpf (apenas números ou formatado), endereco (logradouro, número, complemento e bairro), cidade, estado (sigla com 2 letras) e dataEmissao (formato ISO YYYY-MM-DD). Se não encontrar algum campo, retorne null. Não invente ou alucine informações.
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final BeanOutputConverter<DadosDocumento> outputConverter;

    public DocumentAiService(@Autowired(required = false) ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.outputConverter = new BeanOutputConverter<>(DadosDocumento.class, objectMapper);
    }

    @Override
    public DadosDocumento extrairDados(String textoOcr) {
        return extractDocumentData(textoOcr);
    }

    /**
     * Extrai os dados do comprovante de residência a partir do texto bruto retornado pelo Tesseract OCR.
     *
     * @param rawOcrText Texto bruto do documento
     * @return {@link DadosDocumento} estruturado
     */
    public DadosDocumento extractDocumentData(String rawOcrText) {
        if (rawOcrText == null || rawOcrText.trim().isEmpty()) {
            log.warn("Texto OCR vazio ou nulo recebido para extração estruturada");
            return new DadosDocumento(null, null, null, null, null, null);
        }

        if (chatModel == null) {
            log.warn("ChatModel Spring AI não configurado ou mock. Utilizando extração de fallback.");
            return extractFallback(rawOcrText);
        }

        try {
            String formatInstructions = outputConverter.getFormat();
            String completeSystemPrompt = SYSTEM_PROMPT + "\n\nInstruções obrigatórias de formato de saída:\n" + formatInstructions;

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(completeSystemPrompt),
                    new UserMessage("Texto bruto do comprovante a ser extraído:\n\"\"\"\n" + rawOcrText + "\n\"\"\"")
            ));

            log.info("Enviando requisição de extração estruturada ao Spring AI ChatModel...");
            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new AiProcessingException("O modelo de IA retornou uma resposta nula ou vazia.");
            }

            String content = response.getResult().getOutput().getContent();
            log.debug("Resposta textual bruta recebida da IA: {}", content);

            // Sanitização do conteúdo caso a LLM retorne texto fora do padrão JSON
            String sanitizedJson = sanitizeJson(content);

            DadosDocumento dados = objectMapper.readValue(sanitizedJson, DadosDocumento.class);
            log.info("Dados estruturados com sucesso pela IA: Nome='{}', CPF='{}', Emissão='{}'",
                    dados.nome(), dados.cpf(), dados.dataEmissao());
            return dados;

        } catch (AiProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao comunicar ou desserializar resposta da LLM: {}. Tentando contingência...", ex.getMessage(), ex);
            DadosDocumento fallback = extractFallback(rawOcrText);
            if (fallback != null && (fallback.cpf() != null || fallback.endereco() != null)) {
                log.info("Contingência heurística aplicada com sucesso");
                return fallback;
            }
            throw new AiProcessingException("Falha na extração de dados via IA: " + ex.getMessage(), ex);
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

        // Remove marcadores de código Markdown
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        trimmed = trimmed.trim();

        // Localiza delimitadores do JSON
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    /**
     * Fallback heurístico resiliente via Expressões Regulares para contingência em falhas de API externa.
     */
    public DadosDocumento extractFallback(String text) {
        if (text == null) {
            return new DadosDocumento(null, null, null, null, null, null);
        }

        String cpf = null;
        Matcher cpfMatcher = Pattern.compile("\\b(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})\\b").matcher(text);
        if (cpfMatcher.find()) {
            cpf = cpfMatcher.group(1);
        }

        LocalDate dataEmissao = null;
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
        if (dataEmissao == null) {
            Matcher isoMatcher = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b").matcher(text);
            if (isoMatcher.find()) {
                try {
                    dataEmissao = LocalDate.parse(isoMatcher.group(0));
                } catch (Exception ignored) {
                }
            }
        }

        String nome = null;
        Matcher nomeMatcher = Pattern.compile("(?i)(?:Nome|Titular|Destinat[aá]rio)\\s*[:\\-]\\s*([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,40})").matcher(text);
        if (nomeMatcher.find()) {
            nome = nomeMatcher.group(1).trim();
        }

        String endereco = null;
        Matcher endMatcher = Pattern.compile("(?i)(?:Endere[cç]o|Rua|Av\\.?|Avenida)\\s*[:\\-]?\\s*([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s,.-]{5,80})").matcher(text);
        if (endMatcher.find()) {
            endereco = endMatcher.group(1).trim();
        }

        String estado = null;
        Matcher ufMatcher = Pattern.compile("\\b([A-Z]{2})\\b").matcher(text);
        while (ufMatcher.find()) {
            String candidate = ufMatcher.group(1);
            if ("AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO".contains(candidate)) {
                estado = candidate;
                break;
            }
        }

        String cidade = null;
        Matcher cidadeMatcher = Pattern.compile("(?i)(?:Cidade|Munic[ií]pio)\\s*[:\\-]\\s*([A-Za-zÀ-ÖØ-öø-ÿ\\s]{3,30})").matcher(text);
        if (cidadeMatcher.find()) {
            cidade = cidadeMatcher.group(1).trim();
        }

        return new DadosDocumento(nome, cpf, endereco, cidade, estado, dataEmissao);
    }
}
