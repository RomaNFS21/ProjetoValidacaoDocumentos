import React from 'react';
import {
  FileText,
  FileImage,
  FileCheck2,
  X,
  CheckCircle,
} from 'lucide-react';
import { ArquivoValido } from '../../types/document';

interface SelectedFileCardProps {
  arquivo: ArquivoValido;
  onRemove: () => void;
  disabled?: boolean;
}

export const SelectedFileCard: React.FC<SelectedFileCardProps> = ({
  arquivo,
  onRemove,
  disabled = false,
}) => {
  const isPdf =
    arquivo.extensao === 'PDF' || arquivo.tipoMime === 'application/pdf';

  return (
    <div className="w-full bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-2xs transition-all hover:border-slate-300">
      <div className="flex items-center justify-between gap-4">
        
        {/* Esquerda: Ícone do tipo + Nome + Tamanho + Badge */}
        <div className="flex items-center gap-3.5 min-w-0">
          {/* Ícone estilizado conforme tipo de arquivo */}
          <div
            className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 border ${
              isPdf
                ? 'bg-red-50 text-red-600 border-red-100'
                : 'bg-orange-50 text-[#FF5800] border-[#FFEDD5]'
            }`}
          >
            {isPdf ? (
              <FileText size={24} strokeWidth={2} />
            ) : (
              <FileImage size={24} strokeWidth={2} />
            )}
          </div>

          {/* Dados do arquivo */}
          <div className="flex flex-col min-w-0">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-slate-800 truncate" title={arquivo.nome}>
                {arquivo.nome}
              </span>
              <span className="hidden sm:inline-block px-1.5 py-0.5 text-[10px] font-bold rounded bg-slate-100 text-slate-600 uppercase">
                {arquivo.extensao}
              </span>
            </div>

            <div className="flex items-center gap-2 mt-1">
              <span className="text-xs text-slate-500 font-medium">
                {arquivo.tamanhoFormatado}
              </span>

              <span className="text-slate-300">•</span>

              {/* Badge verde "Arquivo carregado" */}
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                <CheckCircle size={12} className="text-emerald-600" />
                Arquivo carregado
              </span>
            </div>
          </div>
        </div>

        {/* Direita: Botão [X] para remover */}
        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            type="button"
            onClick={onRemove}
            disabled={disabled}
            className="p-2 rounded-xl text-slate-400 hover:text-red-600 hover:bg-red-50 border border-transparent hover:border-red-100 transition-all focus:outline-none focus:ring-2 focus:ring-red-400 cursor-pointer"
            aria-label="Remover arquivo selecionado"
            title="Remover arquivo"
          >
            <X size={20} />
          </button>
        </div>
      </div>

      {/* Mini preview se for imagem */}
      {arquivo.previewUrl && (
        <div className="mt-4 pt-3 border-t border-slate-100 flex items-center gap-3">
          <div className="w-14 h-14 rounded-lg overflow-hidden border border-slate-200 bg-slate-50 flex-shrink-0">
            <img
              src={arquivo.previewUrl}
              alt="Prévia do documento"
              className="w-full h-full object-cover"
            />
          </div>
          <div className="text-xs text-slate-500 flex items-center gap-1.5">
            <FileCheck2 size={16} className="text-emerald-600" />
            <span>Pré-visualização gerada com sucesso. Pronto para extração OCR.</span>
          </div>
        </div>
      )}
    </div>
  );
};

