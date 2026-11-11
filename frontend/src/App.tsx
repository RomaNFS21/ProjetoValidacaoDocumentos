import React, { useState } from 'react';
import { MainLayout } from './components/layout/MainLayout';
import { Stepper } from './components/pipeline/Stepper';
import { DocumentUploadView } from './components/pipeline/DocumentUploadView';
import { ProcessingView } from './components/pipeline/ProcessingView';
import { ResultView } from './components/pipeline/ResultView';
import {
  StepFase,
  ArquivoValido,
  TipoDocumentoCodigo,
  DocumentoRespostaDTO,
} from './types/document';
import { NavItemId } from './types/navigation';
import {
  validarDocumento,
  ApiError,
} from './services/documentoService';
import { AlertCircle, Sparkles } from 'lucide-react';

export const App: React.FC = () => {
  const [activeNav, setActiveNav] = useState<NavItemId>('documentos');
  const [currentStep, setCurrentStep] = useState<StepFase>(1);
  const [uploadedFile, setUploadedFile] = useState<ArquivoValido | null>(null);
  const [documentType, setDocumentType] =
    useState<TipoDocumentoCodigo>('COMPROVANTE_RESIDENCIA');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [resultado, setResultado] = useState<DocumentoRespostaDTO | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isMockAvailable, setIsMockAvailable] = useState<boolean>(false);

  /**
   * Dispara a validação do documento consumindo o endpoint REST /api/v1/documentos/validar
   */
  const handleProcessDocument = async (
    arquivo: ArquivoValido,
    tipo: TipoDocumentoCodigo
  ) => {
    setUploadedFile(arquivo);
    setDocumentType(tipo);
    setIsProcessing(true);
    setErrorMessage(null);
    setIsMockAvailable(false);
    setCurrentStep(2); // Inicia tela animada de processamento

    const startTime = Date.now();
    const MIN_ANIMATION_TIME = 2800; // Tempo mínimo para o usuário visualizar as etapas de OCR & IA

    try {
      // Chamada real ao backend via FormData
      const resposta = await validarDocumento(arquivo.file, tipo);

      // Garante transição suave das animações de OCR e IA
      const elapsedTime = Date.now() - startTime;
      const remainingTime = Math.max(0, MIN_ANIMATION_TIME - elapsedTime);

      setTimeout(() => {
        setResultado(resposta);
        setIsProcessing(false);
        setCurrentStep(4); // Exibe tela de resultado final
      }, remainingTime);
    } catch (err: unknown) {
      const elapsedTime = Date.now() - startTime;
      const remainingTime = Math.max(0, 1500 - elapsedTime);

      setTimeout(() => {
        setIsProcessing(false);
        if (err instanceof ApiError) {
          setErrorMessage(err.message);
          // Se for erro de conexão/servidor offline, disponibiliza opção de visualização simulada
          if (!err.status || err.status >= 500) {
            setIsMockAvailable(true);
          }
        } else if (err instanceof Error) {
          setErrorMessage(err.message);
          setIsMockAvailable(true);
        } else {
          setErrorMessage('Ocorreu um erro inesperado durante a validação.');
        }
        setCurrentStep(1); // Retorna para ajuste ou nova tentativa
      }, remainingTime);
    }
  };

  /**
   * Gera uma resposta mock para fins de demonstração visual caso o backend não esteja ativo
   */
  const handleSimularResultado = (statusAprovado: boolean = true) => {
    const mockResposta: DocumentoRespostaDTO = {
      documentoId: 'b7c3d2e1-4567-489a-bcde-f0123456789a',
      nomeArquivo: uploadedFile?.nome || 'comprovante_residencia.pdf',
      tipoDocumento: documentType,
      status: statusAprovado ? 'APROVADO' : 'REPROVADO',
      mensagem: statusAprovado
        ? 'Documento validado com sucesso após processamento OCR e verificação de regras.'
        : 'Documento reprovado: data de emissão ultrapassa o limite de 90 dias.',
      motivoReprovacao: statusAprovado
        ? null
        : 'Comprovante emitido há mais de 90 dias.',
      criterios: {
        cpfEncontrado: true,
        cpfValido: true,
        enderecoEncontrado: true,
        documentoLegivel: true,
        emissaoMenor90Dias: statusAprovado,
      },
      dadosExtraidos: {
        nome: 'Kleber Meira da Silva',
        cpf: '123.456.789-00',
        endereco: 'Avenida Cais do Apolo, 455, Bairro do Recife',
        cidade: 'Recife',
        estado: 'PE',
        dataEmissao: statusAprovado ? '2026-09-15' : '2025-12-10',
      },
      dataProcessamento: new Date().toISOString(),
    };

    setErrorMessage(null);
    setIsMockAvailable(false);
    setIsProcessing(true);
    setCurrentStep(2);

    setTimeout(() => {
      setResultado(mockResposta);
      setIsProcessing(false);
      setCurrentStep(4);
    }, 2000);
  };

  /**
   * Reseta o fluxo para o início (Passo 1: Upload)
   */
  const handleReset = () => {
    setCurrentStep(1);
    setUploadedFile(null);
    setResultado(null);
    setIsProcessing(false);
    setErrorMessage(null);
    setIsMockAvailable(false);
  };

  return (
    <MainLayout activeNav={activeNav} onNavigate={setActiveNav}>
      <div className="space-y-6">

        {/* 1. Stepper de Progresso em 4 Fases */}
        <Stepper
          currentStep={currentStep}
          onStepClick={(step) => {
            // Permite navegar apenas para etapas concluídas ou atual
            if (step <= currentStep && !isProcessing) {
              setCurrentStep(step);
            }
          }}
        />

        {/* Banner de Erro com Opção de Ação e Simulação se o Backend estiver indisponível */}
        {errorMessage && (
          <div
            role="alert"
            className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-900 shadow-2xs space-y-3 animate-in fade-in-50 duration-200"
          >
            <div className="flex items-start gap-3">
              <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
              <div className="space-y-1 min-w-0 flex-1">
                <h4 className="text-xs font-bold uppercase tracking-wider text-red-800">
                  Atenção no Processamento
                </h4>
                <p className="text-xs leading-relaxed text-red-700">
                  {errorMessage}
                </p>
              </div>
            </div>

            {isMockAvailable && (
              <div className="pt-2 border-t border-red-200/80 flex flex-wrap items-center justify-between gap-2">
                <span className="text-[11px] text-red-700 font-medium">
                  Deseja testar a visualização da tela de resultado com dados simulados?
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleSimularResultado(true)}
                    className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-2xs transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    <Sparkles size={14} />
                    <span>Simular Aprovado</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleSimularResultado(false)}
                    className="px-3 py-1.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-2xs transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    <span>Simular Reprovado</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 2. Conteúdo da Fase Atual */}
        {/* Passo 1: Upload */}
        {currentStep === 1 && (
          <DocumentUploadView
            onProcessDocument={handleProcessDocument}
            onCancel={handleReset}
            isProcessing={isProcessing}
          />
        )}

        {/* Passos 2 e 3: Transição e Carregamento Animado com OpenCV, Tesseract, Spring AI e Java */}
        {(currentStep === 2 || currentStep === 3) && (
          <ProcessingView
            arquivo={uploadedFile}
            onStepChange={(fase) => {
              if (fase === 3 && currentStep === 2) {
                setCurrentStep(3);
              }
            }}
          />
        )}

        {/* Passo 4: Tela de Resultado Final */}
        {currentStep === 4 && resultado && (
          <ResultView
            resultado={resultado}
            onReset={handleReset}
          />
        )}
      </div>
    </MainLayout>
  );
};

export default App;
