package com.portodigital.validacaodocumentos.service;

import java.awt.image.BufferedImage;

/**
 * Contrato de serviço para extração de texto bruto via OCR (Tesseract / Tess4J).
 */
public interface TesseractOcrService {
    String extrairTexto(BufferedImage imagem);
}

