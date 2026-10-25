package com.portodigital.validacaodocumentos.exception;

/**
 * Exceção lançada quando ocorre erro na inicialização ou no processamento OCR
 * (ex: tessdata ausente, falha de reconhecimento, imagem ilegível).
 */
public class OcrProcessingException extends RuntimeException {

    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
