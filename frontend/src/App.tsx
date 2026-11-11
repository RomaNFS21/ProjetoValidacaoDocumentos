import React, { useState } from 'react';
import { MainLayout } from './components/layout/MainLayout';
import { Stepper } from './components/pipeline/Stepper';
import { DocumentUploadView } from './components/pipeline/DocumentUploadView';
import {
  StepFase,
  ArquivoValido,
  TipoDocumentoCodigo,
} from './types/document';
import { NavItemId } from './types/navigation';
import {
  Loader2,
  CheckCircle2,
  AlertCircle,
  FileText,
  RotateCcw,
  Sparkles,
} from 'lucide-react';

export const App: React.FC = () => {
  const [activeNav, setActiveNav] = useState<NavItemId>('documentos');
  const [currentStep, setCurrentStep] = useState<StepFase>(1);
  const [uploadedFile, setUploadedFile] = useState<ArquivoValido | null>(null);
  const [, setDocumentType] =
    useState<TipoDocumentoCodigo>('COMPROVANTE_RESIDENCIA');
  const [isProcessing, setIsProcessing] = useState(false);
  const [feedbackMessage] = useState<string | null>(null);

  // Manipulador quando o usuário clica em "Processar documento →"
  const handleProcessDocument = (
    arquivo: ArquivoValido,
    tipo: TipoDocumentoCodigo
  ) => {
    setUploadedFile(arquivo);
    setDocumentType(tipo);
    setIsProcessing(true);

    // Simulação visual de avanço do pipeline
    setTimeout(() => {
      setIsProcessing(false);
      setCurrentStep(2); // Avança para "Processamento (OCR e extração)"
    }, 1200);
  };

  const handleReset = () => {
    setCurrentStep(1);
    setUploadedFile(null);
    setIsProcessing(false);
  };

  return (
    <MainLayout activeNav={activeNav} onNavigate={setActiveNav}>
      <div className="space-y-6">
        
        {/* 1. Stepper de Progresso em 4 Fases */}
        <Stepper
          currentStep={currentStep}
          onStepClick={(step) => {
            // Permite navegar apenas para etapas concluídas ou atual
            if (step <= currentStep) {
              setCurrentStep(step);
            }
          }}
        />

        {/* 2. Área Principal de Conteúdo Conforme a Etapa */}
        {currentStep === 1 && (
          <DocumentUploadView
            onProcessDocument={handleProcessDocument}
            onCancel={handleReset}
            isProcessing={isProcessing}
          />
        )}

        {/* Placeholder / Visualização para a Etapa 2 (Processamento) */}
        {currentStep === 2 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-2xs text-center space-y-6">
            <div className="w-16 h-16 rounded-2xl bg-orange-50 text-[#FF5800] flex items-center justify-center mx-auto shadow-2xs">
              <Loader2 size={32} className="animate-spin text-[#FF5800]" />
            </div>

            <div className="max-w-md mx-auto">
              <h3 className="text-xl font-bold text-slate-900">
                Processando Documento via OCR
              </h3>
              <p className="text-sm text-slate-600 mt-1.5">
                Executando pipeline OpenCV e Tesseract OCR para extração estruturada de dados com Spring AI.
              </p>
            </div>

            {uploadedFile && (
              <div className="inline-flex items-center gap-3 px-4 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700">
                <FileText size={16} className="text-[#FF5800]" />
                <span className="font-semibold">{uploadedFile.nome}</span>
                <span className="text-slate-400">({uploadedFile.tamanhoFormatado})</span>
              </div>
            )}

            <div className="pt-4 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setCurrentStep(3)}
                className="px-5 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-semibold shadow-2xs transition-all flex items-center gap-2 cursor-pointer"
              >
                <Sparkles size={16} />
                <span>Simular Conclusão OCR (Ir p/ Validação)</span>
              </button>

              <button
                type="button"
                onClick={handleReset}
                className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-600 hover:text-slate-900 text-xs font-semibold transition-all flex items-center gap-2 cursor-pointer"
              >
                <RotateCcw size={14} />
                <span>Reiniciar</span>
              </button>
            </div>
          </div>
        )}

        {/* Placeholder para Etapa 3 (Validação de Regras) */}
        {currentStep === 3 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-2xs text-center space-y-6">
            <div className="w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-2xs">
              <Sparkles size={32} />
            </div>

            <div className="max-w-md mx-auto">
              <h3 className="text-xl font-bold text-slate-900">
                Validação de Regras de Negócio
              </h3>
              <p className="text-sm text-slate-600 mt-1.5">
                Verificando elegibilidade do comprovante, data de emissão inferior a 90 dias e CPF.
              </p>
            </div>

            <div className="pt-4 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setCurrentStep(4)}
                className="px-5 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-semibold shadow-2xs transition-all flex items-center gap-2 cursor-pointer"
              >
                <CheckCircle2 size={16} />
                <span>Finalizar e Ver Resultado</span>
              </button>

              <button
                type="button"
                onClick={handleReset}
                className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-600 hover:text-slate-900 text-xs font-semibold transition-all cursor-pointer"
              >
                Reiniciar
              </button>
            </div>
          </div>
        )}

        {/* Placeholder para Etapa 4 (Resultado Final) */}
        {currentStep === 4 && (
          <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-2xs text-center space-y-6">
            <div className="w-16 h-16 rounded-2xl bg-emerald-50 text-[#22C55E] flex items-center justify-center mx-auto ring-4 ring-emerald-50">
              <CheckCircle2 size={36} />
            </div>

            <div className="max-w-md mx-auto">
              <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 mb-2">
                STATUS: APROVADO
              </span>
              <h3 className="text-xl font-bold text-slate-900">
                Documento Validado com Sucesso!
              </h3>
              <p className="text-sm text-slate-600 mt-1.5">
                Todos os critérios de validação foram atendidos e registrados no PostgreSQL.
              </p>
            </div>

            <div className="pt-4 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={handleReset}
                className="px-6 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-semibold shadow-2xs transition-all flex items-center gap-2 cursor-pointer"
              >
                <RotateCcw size={15} />
                <span>Validar Novo Documento</span>
              </button>
            </div>
          </div>
        )}

        {/* Feedback ou Mensagem de Notificação se houver */}
        {feedbackMessage && (
          <div className="flex items-center gap-2 p-3 rounded-xl bg-blue-50 text-blue-700 text-xs border border-blue-200">
            <AlertCircle size={16} />
            <span>{feedbackMessage}</span>
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default App;
