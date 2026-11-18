import React, { useState } from 'react';
import { MainLayout } from './components/layout/MainLayout';
import { Stepper } from './components/pipeline/Stepper';
import { DocumentUploadView } from './components/pipeline/DocumentUploadView';
import { ProcessingView } from './components/pipeline/ProcessingView';
import { ResultView } from './components/pipeline/ResultView';
import { MeusEnviosView } from './components/views/MeusEnviosView';
import { EquipeView } from './components/views/EquipeView';
import { AjudaView } from './components/views/AjudaView';
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
import { AlertCircle, Sparkles, FileText, ArrowRight } from 'lucide-react';

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
        nome: 'Victor Roma',
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
    setActiveNav('documentos');
  };

  /**
   * Navega para a visualização dos detalhes de um documento histórico
   */
  const handleVerDetalhesHistorico = (item: DocumentoRespostaDTO) => {
    setResultado(item);
    setActiveNav('documentos');
    setCurrentStep(4);
  };

  return (
    <MainLayout
      activeNav={activeNav}
      onNavigate={(id) => {
        setActiveNav(id);
        setErrorMessage(null);
      }}
    >
      <div className="space-y-6">
        
        {/* ======================================================== */}
        {/* 1. ABA: DOCUMENTOS (FLUXO PRINCIPAL DO PIPELINE)        */}
        {/* ======================================================== */}
        {activeNav === 'documentos' && (
          <>
            {/* Stepper de Progresso em 4 Fases */}
            <Stepper
              currentStep={currentStep}
              onStepClick={(step) => {
                // Permite navegar livremente se não estiver em processamento
                if (!isProcessing) {
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
                      O backend ainda está subindo? Você pode testar todas as telas agora mesmo:
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
            {currentStep === 4 && (
              resultado ? (
                <ResultView
                  resultado={resultado}
                  onReset={handleReset}
                />
              ) : (
                <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center space-y-4">
                  <FileText size={40} className="text-slate-300 mx-auto" />
                  <h3 className="text-base font-bold text-slate-800">
                    Nenhum documento processado ainda
                  </h3>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto">
                    Faça o upload de um comprovante no Passo 1 para visualizar o relatório consolidado de regras.
                  </p>
                  <button
                    type="button"
                    onClick={() => setCurrentStep(1)}
                    className="px-5 py-2 rounded-xl bg-[#FF5800] text-white text-xs font-bold hover:bg-[#FF4500] transition-colors"
                  >
                    Ir para o Envio de Arquivo
                  </button>
                </div>
              )
            )}
          </>
        )}

        {/* ======================================================== */}
        {/* 2. ABA: MEUS ENVIOS (HISTÓRICO)                          */}
        {/* ======================================================== */}
        {activeNav === 'meus-envios' && (
          <MeusEnviosView
            onVerDetalhes={handleVerDetalhesHistorico}
            onNovoEnvio={() => {
              handleReset();
              setActiveNav('documentos');
            }}
          />
        )}

        {/* ======================================================== */}
        {/* 3. ABA: RESULTADOS (PAINEL RÁPIDO)                       */}
        {/* ======================================================== */}
        {activeNav === 'resultados' && (
          resultado ? (
            <ResultView
              resultado={resultado}
              onReset={handleReset}
            />
          ) : (
            <div className="bg-white rounded-2xl border border-slate-200 p-8 sm:p-12 text-center space-y-5">
              <div className="w-16 h-16 rounded-2xl bg-orange-50 text-[#FF5800] flex items-center justify-center mx-auto">
                <FileText size={32} />
              </div>
              <div className="max-w-md mx-auto space-y-1.5">
                <h3 className="text-lg font-bold text-slate-900">
                  Nenhum Resultado de Validação Ativo
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Envie um comprovante de residência na aba "Documentos" ou selecione uma validação anterior em "Meus envios".
                </p>
              </div>
              <div className="flex flex-wrap items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => {
                    handleReset();
                    setActiveNav('documentos');
                  }}
                  className="px-5 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-bold uppercase tracking-wider transition-all shadow-2xs flex items-center gap-2 cursor-pointer"
                >
                  <span>Enviar Comprovante</span>
                  <ArrowRight size={15} />
                </button>
                <button
                  type="button"
                  onClick={() => handleSimularResultado(true)}
                  className="px-4 py-2.5 rounded-xl border border-slate-200 hover:border-slate-300 text-slate-700 text-xs font-semibold transition-all flex items-center gap-2 cursor-pointer"
                >
                  <Sparkles size={15} className="text-[#FF5800]" />
                  <span>Ver Demonstração de Resultado</span>
                </button>
              </div>
            </div>
          )
        )}

        {/* ======================================================== */}
        {/* 4. ABA: EQUIPE                                           */}
        {/* ======================================================== */}
        {activeNav === 'equipe' && <EquipeView />}

        {/* ======================================================== */}
        {/* 5. ABA: AJUDA                                            */}
        {/* ======================================================== */}
        {activeNav === 'ajuda' && <AjudaView />}
      </div>
    </MainLayout>
  );
};

export default App;
