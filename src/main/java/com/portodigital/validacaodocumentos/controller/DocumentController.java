package com.portodigital.validacaodocumentos.controller;

import com.portodigital.validacaodocumentos.domain.dto.DocumentoRespostaDTO;
import com.portodigital.validacaodocumentos.domain.enums.StatusValidacao;
import com.portodigital.validacaodocumentos.domain.enums.TipoDocumento;
import com.portodigital.validacaodocumentos.service.DocumentPipelineOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Controller REST para envio, orquestração e consulta de validação documental.
 */
@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos", description = "Endpoints para upload, extração com IA e validação de documentos")
public class DocumentController {

        private final DocumentPipelineOrchestrator orchestrator;

        public DocumentController(DocumentPipelineOrchestrator orchestrator) {
                this.orchestrator = orchestrator;
        }

        @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Valida comprovante de residência", description = "Recebe arquivo via upload Multipart (PNG, JPEG ou PDF) e tipo de documento. Executa o pipeline completo: pré-processamento OpenCV, OCR Tesseract, extração de dados com Spring AI, validação de regras de negócio em Java e persistência no PostgreSQL.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Documento processado com sucesso (status APROVADO ou REPROVADO)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DocumentoRespostaDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Arquivo corrompido, vazio ou requisição inválida", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "413", description = "Arquivo excede o limite máximo permitido de 10MB", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "415", description = "Formato de arquivo não suportado (apenas PNG, JPEG ou PDF)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "500", description = "Falha na execução do OCR ou erro interno no servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "503", description = "Indisponibilidade temporária da API da LLM / Spring AI", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<DocumentoRespostaDTO> validarDocumento(
                        @Parameter(description = "Arquivo binário do comprovante de residência (PNG, JPEG ou PDF)", required = true) @RequestParam("file") MultipartFile file,
                        @Parameter(description = "Tipo do documento enviado", required = false) @RequestParam(value = "tipoDocumento", required = false, defaultValue = "COMPROVANTE_RESIDENCIA") TipoDocumento tipoDocumento) {

                DocumentoRespostaDTO response = orchestrator.processarDocumento(file, tipoDocumento);
                return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Consulta documento por ID", description = "Retorna os detalhes completos, dados extraídos e resultado da validação de um documento processado anteriormente.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Documento encontrado com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DocumentoRespostaDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Documento não encontrado com o ID informado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<DocumentoRespostaDTO> buscarPorId(
                        @Parameter(description = "Identificador único (UUID) do documento", required = true) @PathVariable("id") UUID id) {

                DocumentoRespostaDTO response = orchestrator.buscarPorId(id);
                return ResponseEntity.ok(response);
        }

        @GetMapping(value = "/meus-envios", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Lista histórico de envios paginado", description = "Retorna a listagem paginada dos últimos envios com suporte a paginação e filtro por status (APROVADO ou REPROVADO).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
                        @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<Page<DocumentoRespostaDTO>> listarMeusEnvios(
                        @Parameter(description = "Filtro opcional pelo status da validação (APROVADO / REPROVADO)") @RequestParam(value = "status", required = false) StatusValidacao status,
                        @Parameter(hidden = true) @PageableDefault(sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<DocumentoRespostaDTO> page = orchestrator.listarEnvios(status, pageable);
                return ResponseEntity.ok(page);
        }
}
