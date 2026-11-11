import React, { useState } from 'react';
import {
  ArrowRight,
  FileText,
  ChevronDown,
  RotateCcw,
} from 'lucide-react';
import {
  TipoDocumentoCodigo,
  TIPOS_DOCUMENTO,
  ArquivoValido,
} from '../../types/document';
import { UploadDropzone } from './UploadDropzone';
import { SelectedFileCard } from './SelectedFileCard';

interface DocumentUploadViewProps {
  onProcessDocument: (arquivo: ArquivoValido, tipo: TipoDocumentoCodigo) => void;
  onCancel?: () => void;
  isProcessing?: boolean;
}

export const DocumentUploadView: React.FC<DocumentUploadViewProps> = ({
  onProcessDocument,
  onCancel,
  isProcessing = false,
}) => {
  const [tipoDocumento, setTipoDocumento] =
    useState<TipoDocumentoCodigo>('COMPROVANTE_RESIDENCIA');
  const [arquivoSelecionado, setArquivoSelecionado] =
    useState<ArquivoValido | null>(null);

  const handleFileSelect = (arquivo: ArquivoValido) => {
    setArquivoSelecionado(arquivo);
  };

  const handleRemoveFile = () => {
    if (arquivoSelecionado?.previewUrl) {
      URL.revokeObjectURL(arquivoSelecionado.previewUrl);
    }
    setArquivoSelecionado(null);
  };

  const handleCancel = () => {
    handleRemoveFile();
    setTipoDocumento('COMPROVANTE_RESIDENCIA');
    if (onCancel) {
      onCancel();
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!arquivoSelecionado || isProcessing) return;
    onProcessDocument(arquivoSelecionado, tipoDocumento);
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-2xs space-y-8">
      {/* 1. Título e Subtítulo */}
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
          Enviar comprovante
        </h2>
        <p className="text-sm text-slate-600 mt-1">
          Faça o upload do documento para iniciarmos a extração das informações.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 2. Campo Select/Dropdown: Tipo de Documento */}
        <div className="space-y-2">
          <label
            htmlFor="tipo-documento"
            className="block text-xs font-bold uppercase tracking-wider text-slate-700"
          >
            Tipo de documento <span className="text-[#FF5800]">*</span>
          </label>

          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
              <FileText size={18} />
            </div>

            <select
              id="tipo-documento"
              value={tipoDocumento}
              onChange={(e) =>
                setTipoDocumento(e.target.value as TipoDocumentoCodigo)
              }
              disabled={isProcessing}
              className="w-full pl-10 pr-10 py-3 rounded-xl border border-slate-200 bg-white text-sm font-semibold text-slate-800 shadow-2xs appearance-none focus:outline-none focus:ring-2 focus:ring-[#FF5800] focus:border-[#FF5800] transition-all cursor-pointer disabled:bg-slate-50 disabled:cursor-not-allowed"
            >
              {TIPOS_DOCUMENTO.map((item) => (
                <option key={item.codigo} value={item.codigo}>
                  {item.nome}
                </option>
              ))}
            </select>

            <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-500">
              <ChevronDown size={18} />
            </div>
          </div>

          <p className="text-xs text-slate-600 pl-1">
            {
              TIPOS_DOCUMENTO.find((doc) => doc.codigo === tipoDocumento)
                ?.descricao
            }
          </p>
        </div>

        {/* 3. Área Dropzone ou Card do Arquivo Selecionado */}
        <div className="space-y-2">
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-700">
            Documento digitalizado ou foto <span className="text-[#FF5800]">*</span>
          </label>

          {!arquivoSelecionado ? (
            <UploadDropzone
              onFileSelect={handleFileSelect}
              disabled={isProcessing}
            />
          ) : (
            <SelectedFileCard
              arquivo={arquivoSelecionado}
              onRemove={handleRemoveFile}
              disabled={isProcessing}
            />
          )}
        </div>

        {/* 4. Ações do Rodapé */}
        <div className="pt-6 border-t border-slate-100 flex flex-col-reverse sm:flex-row items-center justify-between gap-4">
          <button
            type="button"
            onClick={handleCancel}
            disabled={isProcessing}
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl border border-slate-200 hover:border-slate-300 text-slate-600 hover:text-slate-800 font-semibold text-xs transition-colors shadow-2xs flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <RotateCcw size={15} />
            <span>Cancelar</span>
          </button>

          <button
            type="submit"
            disabled={!arquivoSelecionado || isProcessing}
            className={`w-full sm:w-auto px-7 py-3 rounded-xl font-bold text-xs uppercase tracking-wider transition-all duration-200 flex items-center justify-center gap-2.5 shadow-2xs ${
              arquivoSelecionado && !isProcessing
                ? 'bg-[#FF5800] hover:bg-[#FF4500] text-white shadow-orange-glow active:scale-98 cursor-pointer'
                : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
            }`}
          >
            <span>{isProcessing ? 'Processando...' : 'Processar documento'}</span>
            <ArrowRight size={16} />
          </button>
        </div>
      </form>
    </div>
  );
};

