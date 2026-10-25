package com.portodigital.validacaodocumentos.util;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utilitário de conversão de alta performance e fidelidade entre BufferedImage (Java AWT)
 * e Mat (OpenCV), suportando canais de escala de cinza (CV_8UC1) e cores BGR (CV_8UC3).
 */
public final class OpenCvImageUtils {

    private static final Logger log = LoggerFactory.getLogger(OpenCvImageUtils.class);

    private OpenCvImageUtils() {
        // Classe utilitária - construtor privado
    }

    /**
     * Converte um {@link BufferedImage} em um {@link Mat} OpenCV.
     * Trata transparência compondo sobre fundo branco e padroniza para BGR (CV_8UC3)
     * ou escala de cinza (CV_8UC1).
     *
     * @param image Imagem de entrada
     * @return Objeto Mat correspondente
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("A imagem de entrada não pode ser nula para conversão em Mat");
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // Se já for escala de cinza simples
            if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                Mat mat = new Mat(height, width, CvType.CV_8UC1);
                byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.put(0, 0, data);
                return mat;
            }

            // Normaliza para TYPE_3BYTE_BGR com fundo branco (elimina canal alfa com segurança)
            BufferedImage bgrImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = bgrImage.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }

            byte[] pixels = ((DataBufferByte) bgrImage.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(height, width, CvType.CV_8UC3);
            mat.put(0, 0, pixels);
            return mat;

        } catch (Exception ex) {
            log.warn("Conversão direta por raster falhou ({}), utilizando fallback via imdecode...", ex.getMessage());
            return fallbackBufferedImageToMat(image);
        }
    }

    /**
     * Converte um {@link Mat} OpenCV de volta para {@link BufferedImage}.
     * Suporta imagens de 1 canal (Grayscale/Binária) e 3 canais (BGR).
     *
     * @param mat Matriz OpenCV de entrada
     * @return BufferedImage pronto para processamento Java AWT ou Tesseract OCR
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            throw new IllegalArgumentException("O Mat de entrada não pode ser nulo nem vazio");
        }

        try {
            int cols = mat.cols();
            int rows = mat.rows();
            int channels = mat.channels();

            int imageType;
            if (channels == 1) {
                imageType = BufferedImage.TYPE_BYTE_GRAY;
            } else if (channels == 3) {
                imageType = BufferedImage.TYPE_3BYTE_BGR;
            } else {
                return fallbackMatToBufferedImage(mat);
            }

            BufferedImage image = new BufferedImage(cols, rows, imageType);
            byte[] buffer = new byte[(int) (mat.total() * channels)];
            mat.get(0, 0, buffer);

            byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
            return image;

        } catch (Exception ex) {
            log.warn("Conversão direta de Mat para BufferedImage falhou ({}), utilizando fallback via imencode...", ex.getMessage());
            return fallbackMatToBufferedImage(mat);
        }
    }

    private static Mat fallbackBufferedImageToMat(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageBytes = baos.toByteArray();
            return Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            throw new IllegalStateException("Falha crítica no fallback de conversão BufferedImage -> Mat", e);
        }
    }

    private static BufferedImage fallbackMatToBufferedImage(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        try {
            Imgcodecs.imencode(".png", mat, matOfByte);
            return ImageIO.read(new ByteArrayInputStream(matOfByte.toArray()));
        } catch (IOException e) {
            throw new IllegalStateException("Falha crítica no fallback de conversão Mat -> BufferedImage", e);
        } finally {
            matOfByte.release();
        }
    }
}
