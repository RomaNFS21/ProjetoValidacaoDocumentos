package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.exception.OcrProcessingException;
import com.portodigital.validacaodocumentos.testutil.TestDocumentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class OcrExtractionServiceTest {

    private OcrExtractionService ocrService;

    @BeforeEach
    void setUp() {
        ocrService = new OcrExtractionService("tessdata", "por", 1, 3);
        ocrService.init();
    }

    @Test
    @DisplayName("Deve extrair texto bruto com sucesso de imagem de teste")
    void shouldExtractTextSuccessfully() {
        BufferedImage image = TestDocumentFactory.createSyntheticComprovanteResidencia(0, false);

        String extractedText = ocrService.extractText(image);

        assertNotNull(extractedText);
        assertFalse(extractedText.isBlank());
        assertTrue(extractedText.contains("COMPROVANTE") || extractedText.contains("RESIDENCIA") || extractedText.contains("RESIDÊNCIA"));
    }

    @Test
    @DisplayName("Deve lançar OcrProcessingException quando a pasta tessdata for inválida")
    void shouldThrowExceptionWhenTessdataDirectoryIsInvalid() {
        OcrExtractionService invalidService = new OcrExtractionService("pasta_inexistente_12345", "por", 1, 3);

        OcrProcessingException exception = assertThrows(
                OcrProcessingException.class,
                invalidService::init
        );

        assertTrue(exception.getMessage().contains("Diretório tessdata não encontrado ou inválido"));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a imagem enviada for nula")
    void shouldThrowExceptionWhenImageIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ocrService.extractText((BufferedImage) null)
        );
    }
}
