package com.portodigital.validacaodocumentos.exception;

import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import com.portodigital.validacaodocumentos.service.OpenCvService;
import com.portodigital.validacaodocumentos.service.SpringAiService;
import com.portodigital.validacaodocumentos.service.TesseractOcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenCvService openCvService;

    @MockBean
    private TesseractOcrService tesseractOcrService;

    @MockBean
    private SpringAiService springAiService;

    @Test
    @DisplayName("Falha no Tesseract OCR deve retornar HTTP 500 com ProblemDetail RFC 7807")
    void shouldReturn500WhenOcrFails() throws Exception {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        when(openCvService.preprocessar(any(MultipartFile.class))).thenReturn(img);
        when(tesseractOcrService.extrairTexto(any(BufferedImage.class)))
                .thenThrow(new OcrProcessingException("Falha na biblioteca nativa do Tesseract"));

        MockMultipartFile file = new MockMultipartFile("file", "doc.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/documentos/validar").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Falha na Execução do OCR"))
                .andExpect(jsonPath("$.detail").value(containsString("Falha ao executar o reconhecimento óptico")));
    }

    @Test
    @DisplayName("Falha na API de IA (Spring AI) deve retornar HTTP 503 com ProblemDetail RFC 7807")
    void shouldReturn503WhenAiServiceFails() throws Exception {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY);
        when(openCvService.preprocessar(any(MultipartFile.class))).thenReturn(img);
        when(tesseractOcrService.extrairTexto(any(BufferedImage.class))).thenReturn("Texto de documento");
        when(springAiService.extrairDados(anyString()))
                .thenThrow(new AiProcessingException("Timeout de conexao com OpenAI / Gemini"));

        MockMultipartFile file = new MockMultipartFile("file", "doc.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/documentos/validar").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Indisponibilidade da API da LLM"))
                .andExpect(jsonPath("$.detail").value(containsString("temporariamente indisponível")));
    }
}

