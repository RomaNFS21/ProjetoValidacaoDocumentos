package com.portodigital.validacaodocumentos.testutil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Fábrica de documentos sintéticos de teste para simular comprovantes de residência
 * com diferentes características (imagem limpa, imagem inclinada/com ruído, PDF, etc.).
 */
public final class TestDocumentFactory {

    private TestDocumentFactory() {}

    /**
     * Cria um BufferedImage sintético de Comprovante de Residência em alta resolução (1200 x 900).
     *
     * @param rotationAngleDegrees Ângulo de inclinação em graus (ex: 0.0 para alinhado, 3.0 para inclinado)
     * @param addNoise             Se verdadeiro, adiciona sombras gradientes e ruído de fundo
     * @return BufferedImage gerado
     */
    public static BufferedImage createSyntheticComprovanteResidencia(double rotationAngleDegrees, boolean addNoise) {
        int width = 1200;
        int height = 900;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Fundo do documento
            g.setColor(new Color(248, 248, 250));
            g.fillRect(0, 0, width, height);

            // Adiciona sombra irregular/gradiente simulando foto de smartphone com iluminação desigual
            if (addNoise) {
                for (int y = 0; y < height; y += 4) {
                    for (int x = 0; x < width; x += 4) {
                        int shadow = (int) (15 * Math.sin(x * 0.005) + 20 * Math.cos(y * 0.005));
                        int grayVal = Math.max(200, Math.min(255, 245 - shadow));
                        g.setColor(new Color(grayVal, grayVal, grayVal));
                        g.fillRect(x, y, 4, 4);
                    }
                }
            }

            // Aplica inclinação se solicitada
            if (Math.abs(rotationAngleDegrees) > 0.001) {
                AffineTransform transform = new AffineTransform();
                transform.rotate(Math.toRadians(rotationAngleDegrees), width / 2.0, height / 2.0);
                g.setTransform(transform);
            }

            // Moldura externa da fatura
            g.setColor(new Color(70, 70, 70));
            g.drawRect(60, 60, width - 120, height - 120);
            g.drawRect(62, 62, width - 124, height - 124);

            // Cabeçalho da Empresa / Concessionária
            g.setColor(new Color(20, 40, 80));
            g.fillRect(70, 70, width - 140, 90);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("COMPROVANTE DE RESIDENCIA", 100, 125);

            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Companhia de Eletricidade e Servicos - Segunda Via", 100, 150);

            // Linha divisória
            g.setColor(new Color(180, 180, 180));
            g.drawLine(80, 180, width - 80, 180);

            // Dados do Titular e Endereço
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString("DADOS DO CLIENTE / CONSUMIDOR", 100, 230);

            g.setFont(new Font("Arial", Font.PLAIN, 20));
            int yPos = 270;
            int lineHeight = 42;

            g.drawString("Nome: Joao da Silva", 100, yPos);
            yPos += lineHeight;

            g.drawString("CPF: 123.456.789-00", 100, yPos);
            yPos += lineHeight;

            g.drawString("Endereco: Rua das Flores, 150", 100, yPos);
            yPos += lineHeight;

            g.drawString("Bairro: Centro", 100, yPos);
            yPos += lineHeight;

            g.drawString("Cidade: Guanambi - BA", 100, yPos);
            yPos += lineHeight;

            g.drawString("Data de emissao: 10/08/2026", 100, yPos);
            yPos += lineHeight;

            g.drawString("Codigo de Instalacao: 00987654321", 100, yPos);

            // Rodapé com código de barras simulado
            g.setColor(new Color(100, 100, 100));
            g.drawString("Autenticacao Digital: 83610000001-2 23450123456-7 89012345678-9", 100, height - 140);

        } finally {
            g.dispose();
        }

        return image;
    }

    /**
     * Cria um {@link MockMultipartFile} contendo o comprovante em formato PNG.
     */
    public static MockMultipartFile createComprovantePngFile(String filename, double rotationAngle, boolean noise)
            throws IOException {
        BufferedImage image = createSyntheticComprovanteResidencia(rotationAngle, noise);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile(
                "file",
                filename,
                "image/png",
                baos.toByteArray()
        );
    }

    /**
     * Cria um {@link MockMultipartFile} contendo o comprovante em formato PDF via Apache PDFBox.
     */
    public static MockMultipartFile createComprovantePdfFile(String filename, double rotationAngle, boolean noise)
            throws IOException {
        BufferedImage image = createSyntheticComprovanteResidencia(rotationAngle, noise);
        byte[] pdfBytes = convertImageToPdfBytes(image);
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                pdfBytes
        );
    }

    private static byte[] convertImageToPdfBytes(BufferedImage image) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
