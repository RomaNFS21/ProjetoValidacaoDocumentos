package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.exception.FileProcessingException;
import com.portodigital.validacaodocumentos.testutil.TestDocumentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentFileServiceTest {

    private DocumentFileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new DocumentFileService(10, 300.0f);
    }

    @Test
    @DisplayName("Deve converter arquivo PNG válido em BufferedImage com sucesso")
    void shouldConvertValidPngToBufferedImage() throws IOException {
        MockMultipartFile file = TestDocumentFactory.createComprovantePngFile("comprovante.png", 0, false);

        BufferedImage image = fileService.convertToImage(file);

        assertNotNull(image);
        assertEquals(1200, image.getWidth());
        assertEquals(900, image.getHeight());
    }

    @Test
    @DisplayName("Deve converter arquivo PDF em BufferedImage a 300 DPI com sucesso")
    void shouldConvertValidPdfToBufferedImageAt300Dpi() throws IOException {
        MockMultipartFile file = TestDocumentFactory.createComprovantePdfFile("comprovante.pdf", 0, false);

        BufferedImage image = fileService.convertToImage(file);

        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    @DisplayName("Deve converter todas as páginas de um PDF")
    void shouldConvertAllPagesOfPdf() throws IOException {
        MockMultipartFile file = TestDocumentFactory.createComprovantePdfFile("comprovante.pdf", 0, false);

        List<BufferedImage> pages = fileService.convertAllPages(file);

        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertNotNull(pages.get(0));
    }

    @Test
    @DisplayName("Deve lançar FileProcessingException quando o arquivo ultrapassar 10MB")
    void shouldThrowExceptionWhenFileExceeds10Mb() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "arquivo_grande.pdf",
                "application/pdf",
                largeBytes
        );

        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> fileService.convertToImage(largeFile)
        );

        assertTrue(exception.getMessage().contains("excede o limite máximo permitido"));
    }

    @Test
    @DisplayName("Deve lançar FileProcessingException quando o formato do arquivo for inválido (ex: .txt ou executável)")
    void shouldThrowExceptionWhenUnsupportedFormat() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "documento.txt",
                "text/plain",
                "conteudo texto".getBytes()
        );

        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> fileService.convertToImage(textFile)
        );

        assertTrue(exception.getMessage().contains("Formato de arquivo não suportado"));
    }

    @Test
    @DisplayName("Deve lançar FileProcessingException quando o arquivo for vazio")
    void shouldThrowExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vazio.png",
                "image/png",
                new byte[0]
        );

        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> fileService.convertToImage(emptyFile)
        );

        assertTrue(exception.getMessage().contains("não pode ser nulo ou vazio"));
    }

    @Test
    @DisplayName("Deve lançar FileProcessingException quando o arquivo for nulo")
    void shouldThrowExceptionWhenFileIsNull() {
        assertThrows(
                FileProcessingException.class,
                () -> fileService.convertToImage(null)
        );
    }
}
