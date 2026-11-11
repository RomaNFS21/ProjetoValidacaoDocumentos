export type TipoDocumentoCodigo =
  | 'COMPROVANTE_RESIDENCIA'
  | 'RG'
  | 'CNH'
  | 'OUTRO';

export type StatusValidacao = 'APROVADO' | 'REPROVADO' | 'EM_PROCESSAMENTO';

export interface TipoDocumentoOption {
  codigo: TipoDocumentoCodigo;
  nome: string;
  descricao?: string;
}

export const TIPOS_DOCUMENTO: TipoDocumentoOption[] = [
  {
    codigo: 'COMPROVANTE_RESIDENCIA',
    nome: 'Comprovante de residência',
    descricao: 'Conta de água, luz, gás, telefone ou fatura recente (máx. 90 dias)',
  },
  {
    codigo: 'RG',
    nome: 'RG - Carteira de Identidade',
    descricao: 'Frente e verso do documento de identidade civil',
  },
  {
    codigo: 'CNH',
    nome: 'CNH - Carteira Nacional de Habilitação',
    descricao: 'Documento aberto com foto legível e QR code',
  },
  {
    codigo: 'OUTRO',
    nome: 'Outro documento oficial',
    descricao: 'Declaração ou comprovante alternativo autenticado',
  },
];

export type StepFase = 1 | 2 | 3 | 4;

export interface StepItem {
  fase: StepFase;
  titulo: string;
  subtitulo: string;
}

export const PIPELINE_STEPS: StepItem[] = [
  {
    fase: 1,
    titulo: 'Upload',
    subtitulo: 'Envio do documento',
  },
  {
    fase: 2,
    titulo: 'Processamento',
    subtitulo: 'OCR e extração',
  },
  {
    fase: 3,
    titulo: 'Validação',
    subtitulo: 'Regras de negócio',
  },
  {
    fase: 4,
    titulo: 'Resultado',
    subtitulo: 'Aprovado ou Reprovado',
  },
];

export interface ArquivoValido {
  file: File;
  nome: string;
  tamanhoFormatado: string;
  extensao: string;
  tipoMime: string;
  previewUrl?: string;
}

export interface CriteriosValidacao {
  cpfEncontrado: boolean;
  cpfValido: boolean;
  enderecoEncontrado: boolean;
  documentoLegivel: boolean;
  emissaoMenor90Dias: boolean;
}

export interface DadosExtraidos {
  nome: string | null;
  cpf: string | null;
  endereco: string | null;
  cidade: string | null;
  estado: string | null;
  dataEmissao: string | null;
}

export interface DocumentoRespostaDTO {
  documentoId: string;
  nomeArquivo: string;
  tipoDocumento: TipoDocumentoCodigo;
  status: StatusValidacao;
  mensagem: string;
  motivoReprovacao?: string | null;
  criterios: CriteriosValidacao;
  dadosExtraidos: DadosExtraidos;
  dataProcessamento: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  [key: string]: unknown;
}
