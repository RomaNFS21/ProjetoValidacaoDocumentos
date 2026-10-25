package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.exception.FileProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Serviço responsável pela recepção, validação estrutural e conversão de arquivos de documentos.
 * Suporta formatos de imagem (PNG, JPEG/JPG) e documentos PDF, convertendo páginas em imagens
 * de alta resolução (300 DPI) para alimentação do pipeline de OCR.
 */
@Service
public class DocumentFileService {

    private static final Logger log = LoggerFactory.getLogger(DocumentFileService.class);

    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    public static final float DEFAULT_PDF_DPI = 300.0f;

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "application/pdf"
    );

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".png",
            ".jpeg",
            ".jpg",
            ".pdf"
    );

    private final long maxFileSizeBytes;
    private final float pdfDpi;

    public DocumentFileService(
            @Value("${ocr.file.max-file-size-mb:10}") long maxFileSizeMb,
            @Value("${ocr.file.pdf-dpi:300}") float pdfDpi) {
        this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        this.pdfDpi = pdfDpi;
        log.info("DocumentFileService inicializado. Limite: {} MB, DPI PDF: {}", maxFileSizeMb, pdfDpi);
    }

    /**
     * Valida e converte o arquivo enviado em uma imagem renderizada em alta resolução.
     * Caso o arquivo seja PDF, converte a primeira página.
     *
     * @param file Arquivo recebido via requisição multipart
     * @return {@link BufferedImage} pronto para o pipeline de visão computacional e OCR
     */
    public BufferedImage convertToImage(MultipartFile file) {
        validateFile(file);

        String contentType = resolveContentType(file);
        log.debug("Processando arquivo '{}' com content-type '{}' e tamanho {} bytes",
                file.getOriginalFilename(), contentType, file.getSize());

        if (isPdf(contentType, file.getOriginalFilename())) {
            return convertPdfFirstPage(file);
        } else {
            return convertRasterImage(file);
        }
    }

    /**
     * Converte todas as páginas de um PDF em uma lista de imagens renderizadas a 300 DPI.
     * Se o arquivo for uma imagem simples (PNG/JPEG), retorna uma lista contendo apenas a imagem.
     *
     * @param file Arquivo recebido via requisição multipart
     * @return Lista de {@link BufferedImage}
     */
    public List<BufferedImage> convertAllPages(MultipartFile file) {
        validateFile(file);

        String contentType = resolveContentType(file);
        if (isPdf(contentType, file.getOriginalFilename())) {
            return convertPdfAllPages(file);
        } else {
            return List.of(convertRasterImage(file));
        }
    }

    /**
     * Valida os requisitos de integridade, limite de tamanho (<= 10MB) e formato permitido.
     *
     * @param file Arquivo a ser validado
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("O arquivo enviado não pode ser nulo ou vazio.");
        }

        if (file.getSize() > this.maxFileSizeBytes) {
            double sizeMb = (double) file.getSize() / (1024 * 1024);
            double limitMb = (double) this.maxFileSizeBytes / (1024 * 1024);
            throw new FileProcessingException(
                    String.format(Locale.ROOT, "Tamanho do arquivo (%.2f MB) excede o limite máximo permitido de %.2f MB.",
                            sizeMb, limitMb)
            );
        }

        String contentType = resolveContentType(file);
        String filename = file.getOriginalFilename();

        boolean validContentType = contentType != null && SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean validExtension = hasSupportedExtension(filename);

        if (!validContentType && !validExtension) {
            throw new FileProcessingException(
                    String.format("Formato de arquivo não suportado: '%s' (tipo: %s). Formatos aceitos: PNG, JPEG e PDF.",
                            filename, contentType)
            );
        }
    }

    /**
     * Converte a primeira página de um documento PDF para BufferedImage em 300 DPI via PDFBox 3.x.
     */
    private BufferedImage convertPdfFirstPage(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.getNumberOfPages() == 0) {
                throw new FileProcessingException("O documento PDF enviado não possui nenhuma página válida.");
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            log.info("Renderizando primeira página do PDF '{}' a {} DPI...", file.getOriginalFilename(), this.pdfDpi);
            return pdfRenderer.renderImageWithDPI(0, this.pdfDpi, ImageType.RGB);

        } catch (IOException e) {
            throw new FileProcessingException("Erro ao ler ou renderizar documento PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Converte todas as páginas de um PDF em imagens a 300 DPI.
     */
    private List<BufferedImage> convertPdfAllPages(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new FileProcessingException("O documento PDF enviado não possui nenhuma página válida.");
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            List<BufferedImage> images = new ArrayList<>(pageCount);

            for (int page = 0; page < pageCount; page++) {
                log.debug("Renderizando página {}/{} do PDF '{}' a {} DPI...", page + 1, pageCount, file.getOriginalFilename(), this.pdfDpi);
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(page, this.pdfDpi, ImageType.RGB);
                images.add(pageImage);
            }

            return images;
        } catch (IOException e) {
            throw new FileProcessingException("Erro ao renderizar páginas do PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Converte um arquivo de imagem (PNG, JPEG) em BufferedImage via ImageIO.
     */
    private BufferedImage convertRasterImage(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new FileProcessingException("Não foi possível decodificar a imagem. O arquivo pode estar corrompido.");
            }
            return image;
        } catch (IOException e) {
            throw new FileProcessingException("Falha de E/S ao ler o fluxo de dados da imagem: " + e.getMessage(), e);
        }
    }

    private boolean isPdf(String contentType, String filename) {
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return true;
        }
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private boolean hasSupportedExtension(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }

        // Inferir a partir da extensão caso o cliente não tenha enviado Content-Type
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".pdf")) return "application/pdf";
        }
        return contentType;
    }
}
