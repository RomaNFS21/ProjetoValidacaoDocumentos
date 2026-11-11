import React, { useState, useRef } from 'react';
import { UploadCloud, AlertCircle } from 'lucide-react';
import { ArquivoValido } from '../../types/document';

interface UploadDropzoneProps {
  onFileSelect: (arquivo: ArquivoValido) => void;
  maxSizeBytes?: number;
  acceptedFormats?: string[];
  disabled?: boolean;
}

const DEFAULT_MAX_SIZE = 10 * 1024 * 1024; // 10MB
const DEFAULT_ACCEPTED = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg'];

export const UploadDropzone: React.FC<UploadDropzoneProps> = ({
  onFileSelect,
  maxSizeBytes = DEFAULT_MAX_SIZE,
  acceptedFormats = DEFAULT_ACCEPTED,
  disabled = false,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const processFile = (file: File) => {
    setErrorMessage(null);

    // Validação de formato
    const isValidFormat =
      acceptedFormats.includes(file.type) ||
      /\.(pdf|png|jpe?g)$/i.test(file.name);

    if (!isValidFormat) {
      setErrorMessage(
        'Formato de arquivo não suportado. Por favor, envie um arquivo PDF, PNG ou JPG.'
      );
      return;
    }

    // Validação de tamanho (máx 10MB)
    if (file.size > maxSizeBytes) {
      setErrorMessage(
        `O arquivo ultrapassa o limite de ${(maxSizeBytes / (1024 * 1024)).toFixed(0)}MB (tamanho atual: ${formatFileSize(file.size)}).`
      );
      return;
    }

    const extensao = file.name.split('.').pop()?.toUpperCase() || 'FILE';
    const previewUrl = file.type.startsWith('image/')
      ? URL.createObjectURL(file)
      : undefined;

    const arquivoValido: ArquivoValido = {
      file,
      nome: file.name,
      tamanhoFormatado: formatFileSize(file.size),
      extensao,
      tipoMime: file.type,
      previewUrl,
    };

    onFileSelect(arquivoValido);
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) {
      setIsDragOver(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    if (disabled) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const droppedFile = e.dataTransfer.files[0];
      processFile(droppedFile);
      e.dataTransfer.clearData();
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      processFile(selectedFile);
    }
  };

  const handleButtonClick = () => {
    if (!disabled && fileInputRef.current) {
      fileInputRef.current.value = '';
      fileInputRef.current.click();
    }
  };

  return (
    <div className="w-full space-y-3">
      {/* Container Dropzone */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={handleButtonClick}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleButtonClick();
          }
        }}
        className={`relative w-full rounded-2xl border-2 border-dashed p-8 sm:p-12 text-center transition-all duration-200 cursor-pointer select-none focus:outline-none focus:ring-2 focus:ring-[#FF5800] focus:ring-offset-2 ${
          isDragOver
            ? 'border-[#FF5800] bg-[#FFF7ED] scale-[1.008] shadow-md shadow-orange-500/10'
            : 'border-slate-300 hover:border-[#FF5800]/80 bg-slate-50/60 hover:bg-[#FFF7ED]/30'
        } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
        aria-label="Área de envio de documentos por clique ou arrastar e soltar"
      >
        {/* Input file invisível */}
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
          onChange={handleFileInputChange}
          disabled={disabled}
          className="hidden"
          aria-hidden="true"
        />

        <div className="max-w-md mx-auto flex flex-col items-center">
          {/* Ícone de nuvem laranja com seta de upload */}
          <div
            className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-transform duration-300 shadow-2xs ${
              isDragOver
                ? 'bg-[#FF5800] text-white scale-110 shadow-orange-glow'
                : 'bg-orange-50 text-[#FF5800] group-hover:scale-105'
            }`}
          >
            <UploadCloud size={32} strokeWidth={2.2} />
          </div>

          {/* Título de Instrução */}
          <h4 className="mt-4 text-base font-bold text-slate-800">
            {isDragOver
              ? 'Solte o arquivo para carregar'
              : 'Arraste e solte o documento aqui'}
          </h4>

          {/* Texto Explicativo com formatos e limite */}
          <p className="text-xs text-slate-500 mt-1 font-medium">
            PDF, PNG, JPG máx. 10MB
          </p>

          {/* Divisor "ou" sutil */}
          <div className="flex items-center gap-3 my-4 w-40">
            <div className="h-px bg-slate-200 flex-1" />
            <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wider">
              ou
            </span>
            <div className="h-px bg-slate-200 flex-1" />
          </div>

          {/* Botão Primário Central "Selecionar arquivo" */}
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              handleButtonClick();
            }}
            disabled={disabled}
            className="px-6 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-semibold shadow-2xs hover:shadow-orange-glow transition-all active:scale-95 cursor-pointer flex items-center gap-2"
          >
            <span>Selecionar arquivo</span>
          </button>
        </div>
      </div>

      {/* Alerta de erro de validação (formato / tamanho) */}
      {errorMessage && (
        <div
          role="alert"
          className="flex items-center gap-2.5 p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-medium animate-in fade-in-50 duration-200"
        >
          <AlertCircle size={18} className="text-red-500 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}
    </div>
  );
};

