import React, { useState } from 'react';
import { MainLayout } from './components/layout/MainLayout';
import { NavItemId } from './types/navigation';
import {
  UploadCloud,
  FileCheck2,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  Info,
} from 'lucide-react';

export const App: React.FC = () => {
  const [activeNav, setActiveNav] = useState<NavItemId>('documentos');

  return (
    <MainLayout activeNav={activeNav} onNavigate={setActiveNav}>
      {/* Cabeçalho da Página / Contexto */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-2xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#FFF7ED] text-[#FF5800] border border-[#FFEDD5]">
                Pipeline OCR + IA
              </span>
              <span className="text-xs text-slate-600 font-medium">
                • Residência Tecnológica
              </span>
            </div>
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Validação de Comprovante de Residência
            </h1>
            <p className="text-sm text-slate-600 mt-1">
              Envie o documento do aluno para extração automatizada de dados via Tesseract OCR e validação de regras com Spring AI.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 text-xs font-semibold flex items-center gap-1.5 border border-emerald-100">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              API Online (v1)
            </div>
          </div>
        </div>

        {/* Indicador visual de etapas do Pipeline */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6">
          <div className="flex items-center gap-3 p-3 rounded-xl bg-[#FFF7ED] border border-[#FFEDD5]">
            <div className="w-8 h-8 rounded-lg bg-[#FF5800] text-white flex items-center justify-center font-bold text-sm shadow-2xs">
              1
            </div>
            <div>
              <p className="text-xs font-bold text-[#9A3412]">Etapa 1: Upload</p>
              <p className="text-[11px] text-slate-600">Envio do PDF ou Imagem</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200 opacity-80">
            <div className="w-8 h-8 rounded-lg bg-slate-200 text-slate-600 flex items-center justify-center font-bold text-sm">
              2
            </div>
            <div>
              <p className="text-xs font-bold text-slate-700">Etapa 2: OCR & IA</p>
              <p className="text-[11px] text-slate-600">OpenCV + Tesseract + LLM</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50 border border-slate-200 opacity-80">
            <div className="w-8 h-8 rounded-lg bg-slate-200 text-slate-600 flex items-center justify-center font-bold text-sm">
              3
            </div>
            <div>
              <p className="text-xs font-bold text-slate-700">Etapa 3: Parecer</p>
              <p className="text-[11px] text-slate-600">Aprovado ou Reprovado</p>
            </div>
          </div>
        </div>
      </div>

      {/* Área de Demonstração / Upload Placeholder */}
      <div className="bg-white rounded-xl border border-slate-200 p-8 shadow-2xs text-center space-y-6">
        <div className="max-w-xl mx-auto border-2 border-dashed border-slate-300 hover:border-[#FF5800] rounded-2xl p-8 transition-colors cursor-pointer group bg-slate-50/50 hover:bg-[#FFF7ED]/30">
          <div className="w-16 h-16 rounded-2xl bg-orange-50 group-hover:bg-[#FFEDD5] text-[#FF5800] flex items-center justify-center mx-auto transition-transform group-hover:scale-110 shadow-2xs">
            <UploadCloud size={32} />
          </div>
          <h3 className="mt-4 text-base font-semibold text-slate-800 group-hover:text-[#FF5800] transition-colors">
            Arraste seu documento aqui ou clique para selecionar
          </h3>
          <p className="text-xs text-slate-600 mt-1.5">
            Suporta arquivos nos formatos <strong>PDF, PNG ou JPEG</strong> de até <strong>10MB</strong>.
          </p>

          <button
            type="button"
            className="mt-5 px-5 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-semibold shadow-2xs hover:shadow-orange-glow transition-all inline-flex items-center gap-2 cursor-pointer"
          >
            <Sparkles size={16} />
            <span>Selecionar Comprovante</span>
          </button>
        </div>

        {/* Recursos de validação integrados */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left max-w-3xl mx-auto pt-4 border-t border-slate-100">
          <div className="flex items-start gap-2.5">
            <div className="p-1 rounded-md bg-emerald-50 text-emerald-600 mt-0.5">
              <ShieldCheck size={16} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-800">Validação Automática</p>
              <p className="text-[11px] text-slate-600">Checagem de CPF, nome e prazo de emissão de 90 dias.</p>
            </div>
          </div>

          <div className="flex items-start gap-2.5">
            <div className="p-1 rounded-md bg-sky-50 text-sky-600 mt-0.5">
              <FileCheck2 size={16} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-800">Tratamento de Imagem</p>
              <p className="text-[11px] text-slate-600">OpenCV realiza binarização, alinhamento e redução de ruído.</p>
            </div>
          </div>

          <div className="flex items-start gap-2.5">
            <div className="p-1 rounded-md bg-purple-50 text-purple-600 mt-0.5">
              <Info size={16} />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-800">Persistência Completa</p>
              <p className="text-[11px] text-slate-600">Histórico de validações registrado no PostgreSQL.</p>
            </div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default App;

