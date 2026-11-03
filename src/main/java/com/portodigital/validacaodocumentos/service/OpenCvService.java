package com.portodigital.validacaodocumentos.service;

import org.springframework.web.multipart.MultipartFile;
import java.awt.image.BufferedImage;

/**
 * Contrato de serviço para conversão de arquivo (PDF, PNG, JPEG) e pré-processamento via OpenCV.
 */
public interface OpenCvService {
    BufferedImage preprocessar(MultipartFile file);
}

