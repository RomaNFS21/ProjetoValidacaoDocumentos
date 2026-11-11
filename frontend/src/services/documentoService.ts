import {
  DocumentoRespostaDTO,
  ProblemDetail,
  TipoDocumentoCodigo,
} from '../types/document';

const API_BASE_URL = '/api/v1/documentos';

export class ApiError extends Error {
  public status?: number;
  public problem?: ProblemDetail;

  constructor(message: string, status?: number, problem?: ProblemDetail) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

/**
 * Envia o arquivo e o tipo de documento via multipart/form-data para a API REST de validação.
 * Endpoint: POST /api/v1/documentos/validar
 */
export async function validarDocumento(
  file: File,
  tipoDocumento: TipoDocumentoCodigo
): Promise<DocumentoRespostaDTO> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('tipoDocumento', tipoDocumento);

  try {
    const response = await fetch(`${API_BASE_URL}/validar`, {
      method: 'POST',
      body: formData,
      headers: {
        Accept: 'application/json',
      },
    });

    if (response.ok) {
      const data: DocumentoRespostaDTO = await response.json();
      return data;
    }

    // Processamento de erro seguindo RFC 7807 (Problem Details)
    let problem: ProblemDetail | undefined;
    let fallbackMessage = `Erro na validação do documento (HTTP ${response.status}).`;

    try {
      problem = await response.json();
      if (problem?.detail) {
        fallbackMessage = problem.detail;
      } else if (problem?.title) {
        fallbackMessage = problem.title;
      }
    } catch {
      // Caso a resposta não seja um JSON válido
    }

    // Tratamento de mensagens amigáveis por código HTTP
    switch (response.status) {
      case 400:
        throw new ApiError(
          problem?.detail ||
            'Arquivo corrompido ou formato ilegível. Verifique o documento e tente novamente.',
          400,
          problem
        );
      case 413:
        throw new ApiError(
          problem?.detail ||
            'O arquivo excede o limite máximo permitido de 10MB.',
          413,
          problem
        );
      case 415:
        throw new ApiError(
          problem?.detail ||
            'Formato de arquivo não suportado. Por favor, envie arquivos em formato PDF, PNG ou JPG.',
          415,
          problem
        );
      case 422:
        throw new ApiError(
          problem?.detail ||
            'Não foi possível extrair dados válidos deste documento.',
          422,
          problem
        );
      case 500:
        throw new ApiError(
          problem?.detail ||
            'Falha interna no motor de processamento OCR ou na análise do documento. Tente novamente mais tarde.',
          500,
          problem
        );
      case 503:
        throw new ApiError(
          problem?.detail ||
            'O serviço de Inteligência Artificial (LLM) está temporariamente indisponível. Aguarde alguns instantes.',
          503,
          problem
        );
      default:
        throw new ApiError(fallbackMessage, response.status, problem);
    }
  } catch (error: unknown) {
    if (error instanceof ApiError) {
      throw error;
    }

    // Erros de conexão / rede
    const errorMessage =
      error instanceof Error ? error.message : 'Erro desconhecido';
    throw new ApiError(
      `Falha na conexão com o servidor de validação. Certifique-se de que a API backend está em execução. Detalhes: ${errorMessage}`
    );
  }
}

/**
 * Realiza o download do relatório consolidado de validação em formato JSON.
 */
export function downloadRelatorioJson(
  resultado: DocumentoRespostaDTO,
  nomeArquivoCustom?: string
): void {
  const jsonString = JSON.stringify(resultado, null, 2);
  const blob = new Blob([jsonString], { type: 'application/json' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  const baseName =
    nomeArquivoCustom ||
    `relatorio_validacao_${resultado.documentoId || Date.now()}`;
  link.download = `${baseName.replace(/\.[^/.]+$/, '')}.json`;

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

