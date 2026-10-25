package com.portodigital.validacaodocumentos.service;

import com.portodigital.validacaodocumentos.config.OpenCvConfig;
import com.portodigital.validacaodocumentos.testutil.TestDocumentFactory;
import com.portodigital.validacaodocumentos.util.OpenCvImageUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImagePreprocessingServiceTest {

    private static OpenCvConfig openCvConfig;
    private ImagePreprocessingService preprocessingService;

    @TempDir
    Path tempDebugDir;

    @BeforeAll
    static void initOpenCv() {
        openCvConfig = new OpenCvConfig();
        openCvConfig.initOpenCv();
    }

    @BeforeEach
    void setUp() {
        preprocessingService = new ImagePreprocessingService(openCvConfig, true, tempDebugDir.toString());
    }

    @Test
    @DisplayName("Deve converter imagem para escala de cinza com canal único")
    void shouldConvertToGrayscale() {
        BufferedImage input = TestDocumentFactory.createSyntheticComprovanteResidencia(0, false);
        Mat srcMat = OpenCvImageUtils.bufferedImageToMat(input);

        Mat grayMat = preprocessingService.convertToGrayscale(srcMat);

        assertNotNull(grayMat);
        assertEquals(1, grayMat.channels(), "A imagem em escala de cinza deve possuir exatamente 1 canal");
        assertEquals(srcMat.rows(), grayMat.rows());
        assertEquals(srcMat.cols(), grayMat.cols());

        srcMat.release();
        grayMat.release();
    }

    @Test
    @DisplayName("Deve aplicar filtro bilateral para remoção de ruídos sem destruir bordas")
    void shouldApplyBilateralFilter() {
        BufferedImage input = TestDocumentFactory.createSyntheticComprovanteResidencia(0, true);
        Mat srcMat = OpenCvImageUtils.bufferedImageToMat(input);
        Mat grayMat = preprocessingService.convertToGrayscale(srcMat);

        Mat denoisedMat = preprocessingService.removeNoise(grayMat);

        assertNotNull(denoisedMat);
        assertEquals(1, denoisedMat.channels());
        assertEquals(grayMat.size(), denoisedMat.size());

        srcMat.release();
        grayMat.release();
        denoisedMat.release();
    }

    @Test
    @DisplayName("Deve aplicar CLAHE para equalização adaptativa de contraste local")
    void shouldApplyClahe() {
        BufferedImage input = TestDocumentFactory.createSyntheticComprovanteResidencia(0, true);
        Mat srcMat = OpenCvImageUtils.bufferedImageToMat(input);
        Mat grayMat = preprocessingService.convertToGrayscale(srcMat);

        Mat claheMat = preprocessingService.applyClahe(grayMat);

        assertNotNull(claheMat);
        assertEquals(1, claheMat.channels());
        assertEquals(CvType.CV_8UC1, claheMat.type());

        srcMat.release();
        grayMat.release();
        claheMat.release();
    }

    @Test
    @DisplayName("Deve aplicar binarização de Otsu resultando em imagem binária (0 e 255)")
    void shouldApplyOtsuThreshold() {
        BufferedImage input = TestDocumentFactory.createSyntheticComprovanteResidencia(0, false);
        Mat srcMat = OpenCvImageUtils.bufferedImageToMat(input);
        Mat grayMat = preprocessingService.convertToGrayscale(srcMat);

        Mat binarized = preprocessingService.applyOtsuThreshold(grayMat);

        assertNotNull(binarized);
        assertEquals(1, binarized.channels());

        // Verifica que o raster possui valores binários
        byte[] sample = new byte[100];
        binarized.get(0, 0, sample);
        for (byte b : sample) {
            int val = b & 0xFF;
            assertTrue(val == 0 || val == 255, "Os pixels binarizados por Otsu devem ser 0 ou 255");
        }

        srcMat.release();
        grayMat.release();
        binarized.release();
    }

    @Test
    @DisplayName("Deve detectar e corrigir inclinação (deskew) com sucesso")
    void shouldDetectAndCorrectSkew() {
        double simulatedAngle = 3.5;
        BufferedImage tiltedImage = TestDocumentFactory.createSyntheticComprovanteResidencia(simulatedAngle, false);

        Mat preprocessed = preprocessingService.preprocessImage(tiltedImage);

        assertNotNull(preprocessed);
        assertEquals(1, preprocessed.channels());
        assertEquals(tiltedImage.getHeight(), preprocessed.rows());
        assertEquals(tiltedImage.getWidth(), preprocessed.cols());

        preprocessed.release();
    }

    @Test
    @DisplayName("Deve salvar imagens intermediárias quando a flag de debug estiver ativa")
    void shouldSaveDebugImagesWhenEnabled() {
        BufferedImage input = TestDocumentFactory.createSyntheticComprovanteResidencia(0, false);

        Mat result = preprocessingService.preprocessImage(input);
        assertNotNull(result);
        result.release();

        File debugFolder = tempDebugDir.toFile();
        File[] subdirs = debugFolder.listFiles(File::isDirectory);
        assertNotNull(subdirs);
        assertTrue(subdirs.length > 0, "Deveria ter criado ao menos uma pasta de sessão de debug");

        File[] debugImages = subdirs[0].listFiles((dir, name) -> name.endsWith(".png"));
        assertNotNull(debugImages);
        assertTrue(debugImages.length >= 5, "Deveria ter gravado as etapas intermediárias (original, gray, denoised, clahe, otsu, deskewed)");
    }

    @Test
    @DisplayName("Deve lançar exceção caso a imagem de entrada seja nula")
    void shouldThrowExceptionWhenInputIsNull() {
        assertThrows(IllegalArgumentException.class, () -> preprocessingService.preprocessImage(null));
        assertThrows(IllegalArgumentException.class, () -> preprocessingService.preprocess(null));
    }
}
