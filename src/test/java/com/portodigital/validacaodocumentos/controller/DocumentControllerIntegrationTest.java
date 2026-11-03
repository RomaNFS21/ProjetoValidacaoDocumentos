package com.portodigital.validacaodocumentos.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.service.DocumentAiService;
import com.portodigital.validacaodocumentos.service.ImagePreprocessingService;
import com.portodigital.validacaodocumentos.service.OcrExtractionService;
import com.portodigital.validacaodocumentos.testutil.TestDocumentFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de Integração da API REST com MockMvc e @SpringBootTest.
 *
 * <p>
 * Estratégia de isolamento:
 * <ul>
 * <li>{@link OcrExtractionService} é mockado para evitar dependência do
 * Tesseract instalado.</li>
 * <li>{@link ImagePreprocessingService} é mockado para evitar dependência do
 * OpenCV nativo.</li>
 * <li>{@link DocumentAiService} é mockado para evitar chamadas reais à
 * OpenAI.</li>
 * <li>O banco de dados usa H2 em memória (perfil "test") com schema gerado pelo
 * Hibernate.</li>
 * </ul>
 *
 * <p>
 * Cobertura de cenários:
 * <ol>
 * <li>Pipeline completo com sucesso → HTTP 200 + status APROVADO + persistência
 * + GET por ID + listagem</li>
 * <li>Documento reprovado por emissão antiga (> 90 dias) → HTTP 200 + status
 * REPROVADO</li>
 * <li>Formato de arquivo não suportado → HTTP 415 + Problem Detail RFC
 * 7807</li>
 * <li>Documento não encontrado por ID → HTTP 404 + Problem Detail RFC 7807</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // ──────────────────────────────────────────────────────────────────────────
        // Mocks das dependências de infraestrutura nativa (OCR, OpenCV, IA)
        // ──────────────────────────────────────────────────────────────────────────

        @MockBean
        private OcrExtractionService ocrExtractionService;

        @MockBean
        private ImagePreprocessingService imagePreprocessingService;

        @MockBean
        private DocumentAiService documentAiService;

        // CPF válido pelo algoritmo Módulo 11
        private static final String CPF_VALIDO = "529.982.247-25";

        // ──────────────────────────────────────────────────────────────────────────
        // Teste 1 – Pipeline completo: APROVADO + persistência + consultas
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("POST /api/v1/documentos/validar - Pipeline completo com sucesso: HTTP 200 + APROVADO + persistência")
        void shouldValidateDocumentSuccessfullyAndReturn200() throws Exception {

                // Stub: pré-processamento OpenCV retorna imagem sintética válida
                BufferedImage imagemStub = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
                when(imagePreprocessingService.preprocessImageToBufferedImage(any(BufferedImage.class)))
                                .thenReturn(imagemStub);

                // Stub: OCR retorna texto bruto simulado com todos os campos necessários
                String textoOcrSimulado = """
                                COMPROVANTE DE RESIDENCIA
                                Nome: Joao da Silva
                                CPF: 529.982.247-25
                                Endereco: Rua das Flores, 150
                                Cidade: Guanambi - BA
                                Data de emissao: %s
                                """.formatted(LocalDate.now().minusDays(15));
                when(ocrExtractionService.extractText(any(BufferedImage.class)))
                                .thenReturn(textoOcrSimulado);

                // Stub: Spring AI retorna dados estruturados válidos e recentes
                DadosDocumento dadosValidos = new DadosDocumento(
                                "Joao da Silva",
                                CPF_VALIDO,
                                "Rua das Flores, 150",
                                "Guanambi",
                                "BA",
                                LocalDate.now().minusDays(15));
                when(documentAiService.extractDocumentData(anyString())).thenReturn(dadosValidos);

                // Monta o arquivo PNG sintético
                MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                                "comprovante_residencia.png",
                                0.0,
                                false);

                // ── Chamada ao endpoint POST /validar ──
                MvcResult result = mockMvc.perform(multipart("/api/v1/documentos/validar")
                                .file(file)
                                .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.documentoId").isNotEmpty())
                                .andExpect(jsonPath("$.nomeArquivo").value("comprovante_residencia.png"))
                                .andExpect(jsonPath("$.tipoDocumento").value("COMPROVANTE_RESIDENCIA"))
                                .andExpect(jsonPath("$.status").value("APROVADO"))
                                .andExpect(jsonPath("$.mensagem").value("Documento validado com sucesso."))
                                .andExpect(jsonPath("$.motivoReprovacao").value(nullValue()))
                                .andExpect(jsonPath("$.criterios.cpfEncontrado").value(true))
                                .andExpect(jsonPath("$.criterios.cpfValido").value(true))
                                .andExpect(jsonPath("$.criterios.enderecoEncontrado").value(true))
                                .andExpect(jsonPath("$.criterios.documentoLegivel").value(true))
                                .andExpect(jsonPath("$.criterios.emissaoMenor90Dias").value(true))
                                .andExpect(jsonPath("$.dadosExtraidos.nome").value("Joao da Silva"))
                                .andExpect(jsonPath("$.dadosExtraidos.cpf").value(CPF_VALIDO))
                                .andExpect(jsonPath("$.dadosExtraidos.endereco").value("Rua das Flores, 150"))
                                .andExpect(jsonPath("$.dadosExtraidos.cidade").value("Guanambi"))
                                .andExpect(jsonPath("$.dadosExtraidos.estado").value("BA"))
                                .andExpect(jsonPath("$.dadosExtraidos.dataEmissao").isNotEmpty())
                                .andExpect(jsonPath("$.dataProcessamento").isNotEmpty())
                                .andReturn();

                // Extrai o documentoId persistido para validar os demais endpoints
                String jsonContent = result.getResponse().getContentAsString();
                JsonNode jsonNode = objectMapper.readTree(jsonContent);
                String documentoId = jsonNode.get("documentoId").asText();

                // ── GET /api/v1/documentos/{id} ──
                mockMvc.perform(get("/api/v1/documentos/{id}", documentoId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.documentoId").value(documentoId))
                                .andExpect(jsonPath("$.status").value("APROVADO"))
                                .andExpect(jsonPath("$.dadosExtraidos.cpf").value(CPF_VALIDO));

                // ── GET /api/v1/documentos/meus-envios?status=APROVADO ──
                mockMvc.perform(get("/api/v1/documentos/meus-envios")
                                .param("status", "APROVADO")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                                .andExpect(jsonPath("$.content[0].status").value("APROVADO"));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Teste 2 – Documento reprovado (emissão > 90 dias)
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("POST /api/v1/documentos/validar - Reprovado por emissão com mais de 90 dias")
        void shouldReturnReprovadoWhenDocumentFailsBusinessRules() throws Exception {

                BufferedImage imagemStub = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
                when(imagePreprocessingService.preprocessImageToBufferedImage(any(BufferedImage.class)))
                                .thenReturn(imagemStub);
                when(ocrExtractionService.extractText(any(BufferedImage.class)))
                                .thenReturn("CPF: 529.982.247-25 Endereco: Rua X Data: 10/01/2020 Comprovante");

                // Documento emitido há 120 dias → viola regra de recência (< 90 dias)
                DadosDocumento dadosAntigos = new DadosDocumento(
                                "Joao da Silva",
                                CPF_VALIDO,
                                "Rua das Flores, 150",
                                "Guanambi",
                                "BA",
                                LocalDate.now().minusDays(120));
                when(documentAiService.extractDocumentData(anyString())).thenReturn(dadosAntigos);

                MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                                "fatura_antiga.png",
                                0.0,
                                false);

                mockMvc.perform(multipart("/api/v1/documentos/validar")
                                .file(file)
                                .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("REPROVADO"))
                                .andExpect(jsonPath("$.motivoReprovacao").value(containsString("superior a 90 dias")))
                                .andExpect(jsonPath("$.criterios.emissaoMenor90Dias").value(false));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Teste 3 – Formato de arquivo não suportado → HTTP 415 (RFC 7807)
        // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/documentos/validar - Formato .txt não suportado → HTTP 415 + Problem Detail")
    void shouldReturn415WhenFileFormatIsNotSupported() throws Exception {

        MockMultipartFile arquivoTexto = new MockMultipartFile(
                "file",
                "comprovante.txt",
                "text/plain",
                "Conteudo de texto simples sem valor documental".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/documentos/validar")
                        .file(arquivoTexto))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.title").value("Formato de Arquivo Não Suportado"))
                .andExpect(jsonPath("$.detail").value(containsString("Formato de arquivo não suportado")));
    }

        // ──────────────────────────────────────────────────────────────────────────
        // Teste 4 – Documento não encontrado → HTTP 404 (RFC 7807)
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("GET /api/v1/documentos/{id} - ID inexistente → HTTP 404 + Problem Detail")
        void shouldReturn404WhenDocumentNotFound() throws Exception {

                UUID idInexistente = UUID.randomUUID();

                mockMvc.perform(get("/api/v1/documentos/{id}", idInexistente)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.title").value("Documento Não Encontrado"))
                                .andExpect(jsonPath("$.detail").value(containsString("Documento não encontrado")));
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Teste 5 – Listagem paginada sem filtro retorna todos os envios
        // ──────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("GET /api/v1/documentos/meus-envios - Listagem paginada sem filtro → HTTP 200 + Page")
        void shouldReturnPagedListWithoutFilter() throws Exception {
                mockMvc.perform(get("/api/v1/documentos/meus-envios")
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
        }
}
