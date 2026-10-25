package com.portodigital.validacaodocumentos.exception;

/**
 * Exceção lançada quando ocorre erro na validação ou conversão de arquivos de documentos
 * (ex: formato não suportado, tamanho excedido, arquivo corrompido).
 */
public class FileProcessingException extends RuntimeException {

    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
