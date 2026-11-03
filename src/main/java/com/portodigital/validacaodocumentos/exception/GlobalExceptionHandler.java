package com.portodigital.validacaodocumentos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Manipulador Global de Exceções da API REST seguindo a especificação RFC 7807 (Problem Details for HTTP APIs).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Arquivo corrompido, vazio ou ilegível -> HTTP 400 (Bad Request)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ProblemDetail> handleFileProcessing(FileProcessingException ex, HttpServletRequest request) {
        log.error("Erro no processamento do arquivo: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Arquivo Corrompido ou Ilegível");
        problem.setType(URI.create("urn:problem-type:arquivo-invalido"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Tamanho do arquivo excedido (> 10MB) -> HTTP 413 (Payload Too Large)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Arquivo excede limite máximo permitido de 10MB: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo enviado excede o limite máximo permitido de 10MB."
        );
        problem.setTitle("Tamanho do Arquivo Excedido");
        problem.setType(URI.create("urn:problem-type:tamanho-excedido"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Falha na execução do Tesseract OCR -> HTTP 500 (Internal Server Error)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(OcrProcessingException.class)
    public ResponseEntity<ProblemDetail> handleOcrFailure(OcrProcessingException ex, HttpServletRequest request) {
        log.error("Falha durante o processamento do Tesseract OCR: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Falha ao executar o reconhecimento óptico de caracteres (Tesseract OCR): " + ex.getMessage()
        );
        problem.setTitle("Falha na Execução do OCR");
        problem.setType(URI.create("urn:problem-type:falha-ocr"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Indisponibilidade da API da LLM / Spring AI -> HTTP 503 (Service Unavailable)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(AiProcessingException.class)
    public ResponseEntity<ProblemDetail> handleAiFailure(AiProcessingException ex, HttpServletRequest request) {
        log.error("Indisponibilidade ou erro na comunicação com a API de IA: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de inteligência artificial (Spring AI) está temporariamente indisponível: " + ex.getMessage()
        );
        problem.setTitle("Indisponibilidade da API da LLM");
        problem.setType(URI.create("urn:problem-type:ia-indisponivel"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Formato de arquivo não suportado -> HTTP 415 (Unsupported Media Type)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(UnsupportedFileFormatException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedFormat(UnsupportedFileFormatException ex, HttpServletRequest request) {
        log.warn("Formato de arquivo não suportado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Formato de Arquivo Não Suportado");
        problem.setType(URI.create("urn:problem-type:formato-nao-suportado"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Documento não encontrado -> HTTP 404 (Not Found)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDocumentNotFound(DocumentNotFoundException ex, HttpServletRequest request) {
        log.warn("Documento não encontrado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Documento Não Encontrado");
        problem.setType(URI.create("urn:problem-type:documento-nao-encontrado"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Parâmetros inválidos de requisição / Bean Validation -> HTTP 400 (Bad Request)
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validação de argumentos rejeitada: {}", errors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Validação de Entrada Rejeitada");
        problem.setType(URI.create("urn:problem-type:requisicao-invalida"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido na requisição: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requisição Inválida");
        problem.setType(URI.create("urn:problem-type:requisicao-invalida"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Fallback genérico para qualquer outra exceção não tratada -> HTTP 500
    // ──────────────────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Erro interno não tratado no servidor: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado durante o processamento do documento."
        );
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("urn:problem-type:erro-interno"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
