package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.config.OpenCvConfig;
import com.portodigital.validacaodocumentos.util.OpenCvImageUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de Pré-processamento de Imagens com OpenCV para Otimização de OCR.
 *
 * Etapas do Pipeline de Visão Computacional:
 * 1. Conversão para Escala de Cinza (Imgproc.cvtColor):
 *    Elimina a dimensionalidade de cores RGB (3 canais), transformando em 1 canal de intensidade
 *    luminosa de 8 bits (0-255), reduzindo consumo de memória e ruídos de cor.
 *
 * 2. Remoção de Ruído com Filtro Bilateral (Imgproc.bilateralFilter):
 *    Diferente do desfoque Gaussiano tradicional que borra arestas de texto, o filtro bilateral
 *    considera tanto a proximidade geométrica no espaço quanto a similaridade radiométrica de intensidade.
 *    Assim, suaviza ruídos de granulação e textura do papel sem degradar o contorno nítido das letras.
 *
 * 3. Equalização Adaptativa de Histograma com Limite de Contraste - CLAHE (Imgproc.createCLAHE):
 *    Divide a imagem em blocos locais (tiles) e equaliza o contraste adaptativamente, aplicando um
 *    teto (clip limit) para não amplificar o ruído. Corrige sombras irregulares comuns em fotos
 *    tiradas com smartphones, amassados no papel e iluminação heterogênea.
 *
 * 4. Binarização de Otsu (Imgproc.threshold com THRESH_BINARY + THRESH_OTSU):
 *    Calcula estatisticamente o limiar ótimo que minimiza a variância intraclasse entre o fundo
 *    claro do documento e os caracteres tipográficos escuros, gerando um mapa binário perfeito (0 ou 255).
 *
 * 5. Detecção e Correção de Inclinação / Deskew (HoughLinesP / minAreaRect):
 *    Calcula a inclinação do texto através da transformada probabilística de Hough e rotaciona a imagem
 *    em torno do centro geométrico com interpolação cúbica para alinhar o documento horizontalmente.
 *
 * 6. Auditoria e Debug:
 *    Se ativada a flag de debug, salva todas as imagens intermediárias em disco para inspeção visual.
 */
@Service
public class ImagePreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private static final double MIN_SKEW_ANGLE_DEG = 0.5;
    private static final double MAX_SKEW_ANGLE_DEG = 45.0;

    private final boolean debugEnabled;
    private final String debugOutputDir;

    public ImagePreprocessingService(
            OpenCvConfig openCvConfig,
            @Value("${ocr.preprocessing.debug.enabled:false}") boolean debugEnabled,
            @Value("${ocr.preprocessing.debug.output-dir:target/ocr-debug}") String debugOutputDir) {
        this.debugEnabled = debugEnabled;
        this.debugOutputDir = debugOutputDir;
        log.info("ImagePreprocessingService pronto. Debug ativado: {}, Diretório de debug: '{}'",
                debugEnabled, debugOutputDir);
    }

    /**
     * Pré-processa uma imagem BufferedImage através do pipeline OpenCV e retorna o {@link Mat} resultante.
     *
     * @param inputImage Imagem Java AWT de entrada
     * @return Matriz OpenCV binarizada, limpa e com rotação corrigida (CV_8UC1)
     */
    public Mat preprocessImage(BufferedImage inputImage) {
        if (inputImage == null) {
            throw new IllegalArgumentException("A imagem de entrada para pré-processamento não pode ser nula");
        }

        Mat sourceMat = OpenCvImageUtils.bufferedImageToMat(inputImage);
        try {
            return preprocess(sourceMat);
        } finally {
            sourceMat.release();
        }
    }

    /**
     * Pré-processa uma imagem BufferedImage e retorna o {@link BufferedImage} resultante pronto para o Tesseract OCR.
     *
     * @param inputImage Imagem Java AWT de entrada
     * @return BufferedImage binarizada e corrigida
     */
    public BufferedImage preprocessImageToBufferedImage(BufferedImage inputImage) {
        Mat processedMat = preprocessImage(inputImage);
        try {
            return OpenCvImageUtils.matToBufferedImage(processedMat);
        } finally {
            processedMat.release();
        }
    }

    /**
     * Executa o pipeline completo de pré-processamento OpenCV sobre a matriz de entrada.
     *
     * @param src Matriz OpenCV original (BGR ou escala de cinza)
     * @return Matriz final binarizada e alinhada (CV_8UC1)
     */
    public Mat preprocess(Mat src) {
        if (src == null || src.empty()) {
            throw new IllegalArgumentException("O Mat de entrada não pode ser nulo nem vazio");
        }

        String executionId = LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "_" + UUID.randomUUID().toString().substring(0, 8);
        log.debug("Iniciando pipeline de pré-processamento OpenCV [ID: {}]", executionId);

        saveDebugImageIfEnabled(src, executionId, "00_original");

        // Etapa 1: Conversão para escala de cinza
        Mat gray = convertToGrayscale(src);
        saveDebugImageIfEnabled(gray, executionId, "01_grayscale");

        // Etapa 2: Remoção de ruído preservando bordas
        Mat denoised = removeNoise(gray);
        gray.release();
        saveDebugImageIfEnabled(denoised, executionId, "02_denoised");

        // Etapa 3: Realce de contraste e correção de sombras via CLAHE
        Mat contrastEnhanced = applyClahe(denoised);
        denoised.release();
        saveDebugImageIfEnabled(contrastEnhanced, executionId, "03_clahe");

        // Etapa 4: Binarização de Otsu
        Mat binarized = applyOtsuThreshold(contrastEnhanced);
        contrastEnhanced.release();
        saveDebugImageIfEnabled(binarized, executionId, "04_otsu_binarized");

        // Etapa 5: Detecção e correção de inclinação (Deskew)
        Mat deskewed = correctSkew(binarized);
        if (deskewed != binarized) {
            binarized.release();
        }
        saveDebugImageIfEnabled(deskewed, executionId, "05_deskewed_final");

        log.debug("Pipeline de pré-processamento finalizado com sucesso [ID: {}]", executionId);
        return deskewed;
    }

    /**
     * Etapa a: Conversão para escala de cinza.
     */
    public Mat convertToGrayscale(Mat src) {
        Mat gray = new Mat();
        if (src.channels() == 1) {
            src.copyTo(gray);
        } else {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        }
        return gray;
    }

    /**
     * Etapa b: Remoção de ruídos.
     * Utiliza o filtro bilateral (bilateralFilter), que analisa a distribuição espacial de Gauss
     * e a similaridade de tons de cinza, mantendo contornos dos caracteres preservados.
     */
    public Mat removeNoise(Mat gray) {
        Mat denoised = new Mat();
        // diâmetro de vizinhança = 9, sigmaColor = 75, sigmaSpace = 75
        Imgproc.bilateralFilter(gray, denoised, 9, 75, 75);
        return denoised;
    }

    /**
     * Etapa c: Ajuste automático de contraste e iluminação com CLAHE.
     * Elimina sombras heterogêneas decorrentes de celulares ou dobras de papel.
     */
    public Mat applyClahe(Mat gray) {
        Mat enhanced = new Mat();
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(gray, enhanced);
        return enhanced;
    }

    /**
     * Etapa d: Binarização adaptativa / limiarização de Otsu.
     * Separa de forma ótima o fundo (papel branco) do primeiro plano (texto preto).
     */
    public Mat applyOtsuThreshold(Mat gray) {
        Mat binarized = new Mat();
        Imgproc.threshold(gray, binarized, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        return binarized;
    }

    /**
     * Etapa e: Detecção e correção de inclinação (Deskew).
     * Calcula o ângulo das linhas de texto via transformada probabilística de Hough e rotaciona
     * o documento caso a inclinação esteja acima do limite perceptível (0.5 graus).
     */
    public Mat correctSkew(Mat binarized) {
        double angle = calculateSkewAngle(binarized);
        log.debug("Ângulo de inclinação detectado: {}°", String.format("%.2f", angle));

        if (Math.abs(angle) < MIN_SKEW_ANGLE_DEG || Math.abs(angle) > MAX_SKEW_ANGLE_DEG) {
            log.debug("Inclinação desprezível ou além da faixa segura (<0.5° ou >45°). Nenhuma rotação necessária.");
            return binarized;
        }

        log.info("Corrigindo inclinação de {}° no documento...", String.format("%.2f", angle));
        Point center = new Point(binarized.cols() / 2.0, binarized.rows() / 2.0);
        Mat rotMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        Mat deskewed = new Mat();
        // BORDER_CONSTANT com 255 preenche os novos cantos rotacionados com a cor de fundo (branco)
        Imgproc.warpAffine(binarized, deskewed, rotMatrix, binarized.size(),
                Imgproc.INTER_CUBIC, Core.BORDER_CONSTANT, new Scalar(255));

        rotMatrix.release();
        return deskewed;
    }

    /**
     * Calcula o ângulo de rotação (skew) da imagem usando a Transformada Probabilística de Hough
     * com fallback para MinAreaRect nos contornos de texto.
     *
     * @param binarized Imagem binarizada (fundo branco, texto preto)
     * @return Ângulo em graus a ser corrigido
     */
    public double calculateSkewAngle(Mat binarized) {
        Mat inverted = new Mat();
        // Inverte para ter texto em branco (pixels 255) em fundo preto (pixels 0)
        Core.bitwise_not(binarized, inverted);

        Mat lines = new Mat();
        try {
            // HoughLinesP(image, lines, rho, theta, threshold, minLineLength, maxLineGap)
            Imgproc.HoughLinesP(inverted, lines, 1, Math.PI / 180, 80, 60, 10);

            List<Double> angles = new ArrayList<>();
            for (int i = 0; i < lines.rows(); i++) {
                double[] lineData = lines.get(i, 0);
                if (lineData == null || lineData.length < 4) continue;

                double x1 = lineData[0];
                double y1 = lineData[1];
                double x2 = lineData[2];
                double y2 = lineData[3];

                double deltaX = x2 - x1;
                double deltaY = y2 - y1;
                double angleDeg = Math.toDegrees(Math.atan2(deltaY, deltaX));

                // Considera apenas linhas horizontais com inclinação de até 45 graus
                if (Math.abs(angleDeg) <= MAX_SKEW_ANGLE_DEG) {
                    angles.add(angleDeg);
                }
            }

            if (!angles.isEmpty()) {
                Collections.sort(angles);
                // Retorna a mediana para descartar outliers (ex: traços verticais ou tabelas)
                return angles.get(angles.size() / 2);
            }

            // Fallback: Se não encontrou linhas retas suficientes, analisa a orientação dos contornos de texto
            return calculateAngleByContours(inverted);

        } finally {
            inverted.release();
            lines.release();
        }
    }

    private double calculateAngleByContours(Mat inverted) {
        Mat nonZeroPoints = new Mat();
        try {
            Core.findNonZero(inverted, nonZeroPoints);
            if (nonZeroPoints.empty()) {
                return 0.0;
            }

            MatOfPoint2f points2f = new MatOfPoint2f();
            nonZeroPoints.convertTo(points2f, org.opencv.core.CvType.CV_32FC2);
            RotatedRect minRect = Imgproc.minAreaRect(points2f);
            points2f.release();

            double angle = minRect.angle;
            if (minRect.size.width < minRect.size.height) {
                angle += 90.0;
            }

            // Normaliza para o intervalo [-45, 45]
            if (angle > 45.0) {
                angle -= 90.0;
            } else if (angle < -45.0) {
                angle += 90.0;
            }

            return (Math.abs(angle) <= MAX_SKEW_ANGLE_DEG) ? angle : 0.0;
        } catch (Exception ex) {
            log.debug("Fallback de deskew por contornos ignorado: {}", ex.getMessage());
            return 0.0;
        } finally {
            nonZeroPoints.release();
        }
    }

    /**
     * Salva uma imagem intermediária para auditoria e diagnóstico visual se a flag de debug estiver ativa.
     */
    private void saveDebugImageIfEnabled(Mat mat, String executionId, String stepName) {
        if (!debugEnabled) {
            return;
        }

        try {
            Path outputDirectory = Paths.get(debugOutputDir, executionId);
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }

            File outputFile = outputDirectory.resolve(stepName + ".png").toFile();
            Imgcodecs.imwrite(outputFile.getAbsolutePath(), mat);
            log.debug("Imagem de auditoria salva: {}", outputFile.getAbsolutePath());

        } catch (IOException e) {
            log.warn("Não foi possível salvar imagem de debug ({}/{}): {}", executionId, stepName, e.getMessage());
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getDebugOutputDir() {
        return debugOutputDir;
    }
}
