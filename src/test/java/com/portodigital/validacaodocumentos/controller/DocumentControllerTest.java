package com.portodigital.validacaodocumentos.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portodigital.validacaodocumentos.domain.dto.DadosDocumento;
import com.portodigital.validacaodocumentos.service.OpenCvService;
import com.portodigital.validacaodocumentos.service.SpringAiService;
import com.portodigital.validacaodocumentos.service.TesseractOcrService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do controlador REST {@link DocumentController}.
 * Valida a orquestração do pipeline multipart, os contratos da API e a persistência em banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock das dependências externas / lentas (Visão Computacional, OCR e LLM)
    @MockBean
    private OpenCvService openCvService;

    @MockBean
    private TesseractOcrService tesseractOcrService;

    @MockBean
    private SpringAiService springAiService;

    private static final String CPF_VALIDO = "529.982.247-25";

    @Test
    @DisplayName("POST /api/v1/documentos/validar - Upload Multipart e validação completa com HTTP 200")
    void deveValidarDocumentoMultipartComSucessoERetornar200() throws Exception {
        // 1. Mock do pré-processamento OpenCV (retorna imagem sintética válida)
        BufferedImage imagemStub = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        when(openCvService.preprocessar(any(MultipartFile.class))).thenReturn(imagemStub);

        // 2. Mock do Tesseract OCR (retorna texto simulado do comprovante)
        String textoOcrSimulado = """
                COMPROVANTE DE RESIDENCIA
                Nome: Joao da Silva
                CPF: 529.982.247-25
                Endereco: Rua das Flores, 150
                Cidade: Guanambi - BA
                Data de emissao: %s
                """.formatted(LocalDate.now().minusDays(10));
        when(tesseractOcrService.extrairTexto(any(BufferedImage.class))).thenReturn(textoOcrSimulado);

        // 3. Mock da extração estruturada via Spring AI (LLM)
        DadosDocumento dadosExtraidos = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(10)
        );
        when(springAiService.extrairDados(anyString())).thenReturn(dadosExtraidos);

        // 4. Criação do arquivo sintético PNG para o multipart
        MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                "comprovante_residencia.png",
                0.0,
                false
        );

        // 5. Execução da chamada multipart POST /api/v1/documentos/validar via MockMvcRequestBuilders
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/documentos/validar")
                        .file(file)
                        .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                .andDo(print())
                .andExpect(status().isOk())
                // Assert dos campos da raiz do JSON de DocumentoRespostaDTO
                .andExpect(jsonPath("$.documentoId").isNotEmpty())
                .andExpect(jsonPath("$.nomeArquivo").value("comprovante_residencia.png"))
                .andExpect(jsonPath("$.tipoDocumento").value("COMPROVANTE_RESIDENCIA"))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.mensagem").value("Documento validado com sucesso."))
                // Assert dos critérios avaliados
                .andExpect(jsonPath("$.criterios.cpfEncontrado").value(true))
                .andExpect(jsonPath("$.criterios.cpfValido").value(true))
                .andExpect(jsonPath("$.criterios.enderecoEncontrado").value(true))
                .andExpect(jsonPath("$.criterios.documentoLegivel").value(true))
                .andExpect(jsonPath("$.criterios.emissaoMenor90Dias").value(true))
                // Assert dos dados extraídos pela IA
                .andExpect(jsonPath("$.dadosExtraidos.nome").value("Joao da Silva"))
                .andExpect(jsonPath("$.dadosExtraidos.cpf").value(CPF_VALIDO))
                .andExpect(jsonPath("$.dadosExtraidos.endereco").value("Rua das Flores, 150"))
                .andExpect(jsonPath("$.dadosExtraidos.cidade").value("Guanambi"))
                .andExpect(jsonPath("$.dadosExtraidos.estado").value("BA"))
                .andExpect(jsonPath("$.dadosExtraidos.dataEmissao").isNotEmpty())
                .andExpect(jsonPath("$.dataProcessamento").isNotEmpty())
                .andReturn();

        // 6. Validação dos endpoints de consulta com o ID retornado
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        String documentoId = json.get("documentoId").asText();

        // GET /api/v1/documentos/{id}
        mockMvc.perform(get("/api/v1/documentos/{id}", documentoId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value(documentoId))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.dadosExtraidos.cpf").value(CPF_VALIDO));

        // GET /api/v1/documentos/meus-envios
        mockMvc.perform(get("/api/v1/documentos/meus-envios")
                        .param("status", "APROVADO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].documentoId").value(documentoId));
    }

    @Test
    @DisplayName("GET /api/v1/documentos/{id} - Documento inexistente deve retornar HTTP 404 RFC 7807")
    void deveRetornar404QuandoDocumentoNaoExistir() throws Exception {
        UUID idInexistente = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/documentos/{id}", idInexistente)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Documento Não Encontrado"))
                .andExpect(jsonPath("$.detail").value(containsString("Documento não encontrado")));
    }
}

