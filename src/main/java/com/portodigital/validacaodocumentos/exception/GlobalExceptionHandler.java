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
 * Tratamento Global de Erros seguindo a especificação RFC 7807 (Problem Details
 * for HTTP APIs).
 *
 * <p>
 * Todos os erros retornam o payload padronizado {@link ProblemDetail} com os
 * campos:
 * <ul>
 * <li>{@code type} – URI que identifica a classe do problema</li>
 * <li>{@code title} – Rótulo legível por humanos</li>
 * <li>{@code status} – Código HTTP numérico</li>
 * <li>{@code detail} – Descrição específica desta ocorrência</li>
 * <li>{@code instance} – URI da requisição que gerou o erro</li>
 * <li>{@code timestamp} – Instante UTC da ocorrência</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ──────────────────────────────────────────────────────────────────────────
    // 404 – Documento não encontrado
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(DocumentNotFoundException.class)

    public ResponseEntity<ProblemDetail> handleDocumentNotFound(
            DocumentNotFoundException ex, HttpServletRequest request) {

        log.warn("Documento não encontrado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Documento Não Encontrado");
        problem.setType(URI.create("urn:problem-type:documento-nao-encontrado"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 415 – Formato de arquivo não suportado
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(UnsupportedFileFormatException.class)

    public ResponseEntity<ProblemDetail> handleUnsupportedFormat(
            UnsupportedFileFormatException ex, HttpServletRequest request) {

        log.warn("Formato de arquivo não suportado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Formato de Arquivo Não Suportado");
        problem.setType(URI.create("urn:problem-type:formato-nao-suportado"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 400 – Arquivo corrompido ou inválido
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(FileProcessingException.class)

    public ResponseEntity<ProblemDetail> handleFileProcessing(
            FileProcessingException ex, HttpServletRequest request) {

        log.error("Erro no processamento do arquivo: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Arquivo Corrompido ou Inválido");
        problem.setType(URI.create("urn:problem-type:arquivo-invalido"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 413 – Arquivo excede 10 MB
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)

    public ResponseEntity<ProblemDetail> handleMaxSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("Arquivo excede limite de 10 MB: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo enviado excede o limite máximo permitido de 10 MB."
        );
        problem.setTitle("Tamanho do Arquivo Excedido");
        problem.setType(URI.create("urn:problem-type:tamanho-excedido"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 422 – Falha no Tesseract OCR
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(OcrProcessingException.class)

    public ResponseEntity<ProblemDetail> handleOcrFailure(
            OcrProcessingException ex, HttpServletRequest request) {

        log.error("Falha no Tesseract OCR: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Falha na Extração OCR");
        problem.setType(URI.create("urn:problem-type:falha-ocr"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 503 – Indisponibilidade da API da LLM / Spring AI
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(AiProcessingException.class)

    public ResponseEntity<ProblemDetail> handleAiFailure(
            AiProcessingException ex, HttpServletRequest request) {

        log.error("Erro na comunicação com o modelo de IA: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de IA está temporariamente indisponível para extração: " + ex.getMessage());
        problem.setTitle("Indisponibilidade da API da LLM");
        problem.setType(URI.create("urn:problem-type:ia-indisponivel"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 400 – Parâmetros de requisição inválidos (Bean Validation)
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Parâmetros inválidos na requisição: {}", errors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Validação de Entrada Rejeitada");
        problem.setType(URI.create("urn:problem-type:requisicao-invalida"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 400 – IllegalArgumentException
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)

    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Argumento inválido recebido: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requisição Inválida");
        problem.setType(URI.create("urn:problem-type:requisicao-invalida"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 500 – Fallback genérico (qualquer Exception não tratada acima)
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)

    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Erro interno não tratado durante o processamento: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado durante o processamento do documento.");
        problem.setTitle("Erro Interno do Servidor");
        problem.setType(URI.create("urn:problem-type:erro-interno"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
