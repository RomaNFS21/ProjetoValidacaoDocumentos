package com.portodigital.validacaodocumentos.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;

/**
 * Implementação padrão de {@link OpenCvService} integrando DocumentFileService e ImagePreprocessingService.
 */
@Service
public class OpenCvServiceImpl implements OpenCvService {

    private final DocumentFileService documentFileService;
    private final ImagePreprocessingService imagePreprocessingService;

    public OpenCvServiceImpl(DocumentFileService documentFileService, ImagePreprocessingService imagePreprocessingService) {
        this.documentFileService = documentFileService;
        this.imagePreprocessingService = imagePreprocessingService;
    }

    @Override
    public BufferedImage preprocessar(MultipartFile file) {
        BufferedImage originalImage = documentFileService.convertToImage(file);
        return imagePreprocessingService.preprocessImageToBufferedImage(originalImage);
    }
}

