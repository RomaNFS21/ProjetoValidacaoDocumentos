package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.config.OpenCvConfig;
import com.portodigital.validacaodocumentos.testutil.TestDocumentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste Automatizado de Integração do Pipeline de Documentos (Etapas 1, 2 e 3):
 * Validação de ponta a ponta:
 * Input (MultipartFile PNG ou PDF) -> DocumentFileService -> ImagePreprocessingService (OpenCV) -> OcrExtractionService (Tess4J) -> Texto Bruto (String).
 */
@SpringBootTest
@ActiveProfiles("test")
class DocumentPipelineIntegrationTest {

    @Autowired
    private DocumentFileService documentFileService;

    @Autowired
    private ImagePreprocessingService imagePreprocessingService;

    @Autowired
    private OcrExtractionService ocrExtractionService;

    @Test
    @DisplayName("Fluxo Completo de Ponta a Ponta: Upload de Imagem PNG com Ruído e Inclinação -> OpenCV -> Tesseract -> Texto Bruto")
    void shouldExecuteFullPipelineForPngDocument() throws Exception {
        // 1. Simula recepção de arquivo PNG de comprovante de residência com ruído e leve inclinação (2.5 graus)
        MockMultipartFile file = TestDocumentFactory.createComprovantePngFile(
                "comprovante_residencia.png",
                2.5,
                true
        );

        // 2. Etapa 1: Recepção e Conversão do Arquivo
        BufferedImage originalImage = documentFileService.convertToImage(file);
        assertNotNull(originalImage, "A imagem convertida do arquivo não deve ser nula");
        assertEquals(1200, originalImage.getWidth());
        assertEquals(900, originalImage.getHeight());

        // 3. Etapa 2: Pré-processamento com OpenCV (Gray -> Bilateral -> CLAHE -> Otsu -> Deskew)
        BufferedImage preprocessedImage = imagePreprocessingService.preprocessImageToBufferedImage(originalImage);
        assertNotNull(preprocessedImage, "A imagem pré-processada pelo OpenCV não deve ser nula");
        assertEquals(originalImage.getWidth(), preprocessedImage.getWidth());
        assertEquals(originalImage.getHeight(), preprocessedImage.getHeight());

        // 4. Etapa 3: Extração de Texto via Tesseract OCR
        String rawText = ocrExtractionService.extractText(preprocessedImage);
        assertNotNull(rawText, "O texto extraído pelo OCR não deve ser nulo");
        assertFalse(rawText.isBlank(), "O texto extraído pelo OCR não deve estar em branco");

        System.out.println("=================================================");
        System.out.println("TEXTO BRUTO EXTRAÍDO VIA OCR (PNG):");
        System.out.println(rawText);
        System.out.println("=================================================");

        // 5. Validação das palavras-chave essenciais do Comprovante de Residência
        String upperText = rawText.toUpperCase();
        assertTrue(upperText.contains("COMPROVANTE"), "Deve conter a palavra COMPROVANTE");
        assertTrue(upperText.contains("RESID"), "Deve conter o radical de RESIDÊNCIA");
        assertTrue(upperText.contains("SILVA") || upperText.contains("JOAO"), "Deve conter o nome do titular (João da Silva)");
        assertTrue(upperText.contains("123") || upperText.contains("456"), "Deve conter dígitos do CPF");
        assertTrue(upperText.contains("FLORES"), "Deve conter o logradouro Rua das Flores");
        assertTrue(upperText.contains("GUANAMBI") || upperText.contains("BA"), "Deve conter a cidade e estado Guanambi - BA");
    }

    @Test
    @DisplayName("Fluxo Completo de Ponta a Ponta: Upload de Documento PDF -> Renderização 300 DPI (PDFBox) -> OpenCV -> Tesseract")
    void shouldExecuteFullPipelineForPdfDocument() throws Exception {
        // 1. Simula recepção de arquivo PDF de comprovante de residência
        MockMultipartFile file = TestDocumentFactory.createComprovantePdfFile(
                "fatura_energia.pdf",
                0.0,
                false
        );

        // 2. Etapa 1: Conversão da primeira página do PDF a 300 DPI
        BufferedImage pdfPageImage = documentFileService.convertToImage(file);
        assertNotNull(pdfPageImage, "A imagem renderizada a partir do PDF não deve ser nula");
        assertTrue(pdfPageImage.getWidth() > 0);
        assertTrue(pdfPageImage.getHeight() > 0);

        // 3. Etapa 2: Pré-processamento com OpenCV
        BufferedImage preprocessed = imagePreprocessingService.preprocessImageToBufferedImage(pdfPageImage);
        assertNotNull(preprocessed);

        // 4. Etapa 3: Extração OCR com Tesseract
        String rawText = ocrExtractionService.extractText(preprocessed);
        assertNotNull(rawText);
        assertFalse(rawText.isBlank());

        System.out.println("=================================================");
        System.out.println("TEXTO BRUTO EXTRAÍDO VIA OCR (PDF a 300 DPI):");
        System.out.println(rawText);
        System.out.println("=================================================");

        String upperText = rawText.toUpperCase();
        assertTrue(upperText.contains("COMPROVANTE"));
        assertTrue(upperText.contains("SILVA") || upperText.contains("JOAO"));
        assertTrue(upperText.contains("FLORES"));
    }
}
