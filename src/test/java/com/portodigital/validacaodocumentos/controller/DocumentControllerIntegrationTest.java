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
import org.springframework.web.multipart.MultipartFile;

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
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenCvService openCvService;

    @MockBean
    private TesseractOcrService tesseractOcrService;

    @MockBean
    private SpringAiService springAiService;

    private static final String CPF_VALIDO = "529.982.247-25";

    @Test
    @DisplayName("POST /api/v1/documentos/validar - Pipeline completo com sucesso: HTTP 200 + APROVADO + persistência")
    void shouldValidateDocumentSuccessfullyAndReturn200() throws Exception {

        BufferedImage imagemStub = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        when(openCvService.preprocessar(any(MultipartFile.class))).thenReturn(imagemStub);

        String textoOcrSimulado = """
                COMPROVANTE DE RESIDENCIA
                Nome: Joao da Silva
                CPF: 529.982.247-25
                Endereco: Rua das Flores, 150
                Cidade: Guanambi - BA
                Data de emissao: %s
                """.formatted(LocalDate.now().minusDays(15));
        when(tesseractOcrService.extrairTexto(any(BufferedImage.class))).thenReturn(textoOcrSimulado);

        DadosDocumento dadosValidos = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(15)
        );
        when(springAiService.extrairDados(anyString())).thenReturn(dadosValidos);

        MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                "comprovante_residencia.png",
                0.0,
                false
        );

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

        String jsonContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(jsonContent);
        String documentoId = jsonNode.get("documentoId").asText();

        // GET /api/v1/documentos/{id}
        mockMvc.perform(get("/api/v1/documentos/{id}", documentoId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value(documentoId))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.dadosExtraidos.cpf").value(CPF_VALIDO));

        // GET /api/v1/documentos/meus-envios?status=APROVADO
        mockMvc.perform(get("/api/v1/documentos/meus-envios")
                        .param("status", "APROVADO")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].status").value("APROVADO"));
    }

    @Test
    @DisplayName("POST /api/v1/documentos/validar - Reprovado por emissão com mais de 90 dias")
    void shouldReturnReprovadoWhenDocumentFailsBusinessRules() throws Exception {

        BufferedImage imagemStub = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        when(openCvService.preprocessar(any(MultipartFile.class))).thenReturn(imagemStub);
        when(tesseractOcrService.extrairTexto(any(BufferedImage.class)))
                .thenReturn("CPF: 529.982.247-25 Endereco: Rua X Data: 10/01/2020 Comprovante");

        DadosDocumento dadosAntigos = new DadosDocumento(
                "Joao da Silva",
                CPF_VALIDO,
                "Rua das Flores, 150",
                "Guanambi",
                "BA",
                LocalDate.now().minusDays(120)
        );
        when(springAiService.extrairDados(anyString())).thenReturn(dadosAntigos);

        MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                "fatura_antiga.png",
                0.0,
                false
        );

        mockMvc.perform(multipart("/api/v1/documentos/validar")
                        .file(file)
                        .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPROVADO"))
                .andExpect(jsonPath("$.motivoReprovacao").value(containsString("superior a 90 dias")))
                .andExpect(jsonPath("$.criterios.emissaoMenor90Dias").value(false));
    }

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
