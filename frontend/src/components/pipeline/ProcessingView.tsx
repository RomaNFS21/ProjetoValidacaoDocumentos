import React, { useEffect, useState } from 'react';
import {
  Scan,
  FileSearch,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
  FileText,
  LucideIcon,
} from 'lucide-react';
import { ArquivoValido } from '../../types/document';

interface ProcessingViewProps {
  arquivo: ArquivoValido | null;
  onStepChange?: (fase: 2 | 3) => void;
}

interface EtapaProcessamento {
  id: number;
  mensagem: string;
  fase: 2 | 3;
  icone: LucideIcon;
  descricao: string;
}

const ETAPAS: EtapaProcessamento[] = [
  {
    id: 0,
    mensagem: 'Tratando imagem com OpenCV...',
    fase: 2,
    icone: Scan,
    descricao: 'Redução de ruído, binarização adaptativa e correção de perspectiva',
  },
  {
    id: 1,
    mensagem: 'Lendo caracteres com Tesseract OCR...',
    fase: 2,
    icone: FileSearch,
    descricao: 'Reconhecimento óptico de caracteres em português (pt-BR)',
  },
  {
    id: 2,
    mensagem: 'Estruturando dados com IA...',
    fase: 2,
    icone: Sparkles,
    descricao: 'Spring AI e LLM extraindo Nome, CPF, Endereço e Data de Emissão',
  },
  {
    id: 3,
    mensagem: 'Validando regras de negócio em Java...',
    fase: 3,
    icone: ShieldCheck,
    descricao: 'Checagem de consistência de CPF, titularidade e emissão nos últimos 90 dias',
  },
];

export const ProcessingView: React.FC<ProcessingViewProps> = ({
  arquivo,
  onStepChange,
}) => {
  const [etapaAtualIndex, setEtapaAtualIndex] = useState<number>(0);

  useEffect(() => {
    // Alterna sequencialmente as mensagens a cada 1.8 segundos enquanto o processamento ocorre
    const interval = setInterval(() => {
      setEtapaAtualIndex((prev) => {
        const next = (prev + 1) % ETAPAS.length;
        if (onStepChange) {
          onStepChange(ETAPAS[next].fase);
        }
        return next;
      });
    }, 1800);

    return () => clearInterval(interval);
  }, [onStepChange]);

  const etapaAtual = ETAPAS[etapaAtualIndex];
  const IconeEtapa = etapaAtual.icone;
  const porcentagemProgresso = Math.min(
    Math.round(((etapaAtualIndex + 1) / ETAPAS.length) * 100),
    95
  );

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-10 shadow-2xs space-y-8 animate-in fade-in-50 duration-300">
      
      {/* Cabeçalho de Status */}
      <div className="text-center max-w-lg mx-auto space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold bg-[#FFF7ED] text-[#FF5800] border border-[#FFEDD5]">
          <span className="w-2 h-2 rounded-full bg-[#FF5800] animate-ping" />
          <span>Etapa {etapaAtual.fase === 2 ? '2: OCR e Extração' : '3: Validação'}</span>
        </div>
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
          Analisando Comprovante
        </h2>
        <p className="text-xs sm:text-sm text-slate-500">
          Aguarde enquanto o pipeline executa o tratamento visual, extração OCR e validação de regras.
        </p>
      </div>

      {/* Animação Central com Ícone Dinâmico */}
      <div className="relative flex flex-col items-center justify-center py-6">
        {/* Glow de fundo */}
        <div className="absolute w-36 h-36 rounded-full bg-orange-100/60 blur-xl animate-pulse" />

        {/* Anel Externo Giratório */}
        <div className="relative w-28 h-28 flex items-center justify-center">
          <div className="absolute inset-0 rounded-full border-4 border-slate-100 border-t-[#FF5800] border-r-[#FF5800]/50 animate-spin" />
          
          {/* Círculo Interno com o Ícone da Etapa */}
          <div className="w-20 h-20 rounded-full bg-white shadow-card flex items-center justify-center text-[#FF5800] border border-[#FFEDD5] z-10 transition-transform duration-300 transform scale-100">
            <IconeEtapa size={36} className="animate-in zoom-in-75 duration-300" />
          </div>
        </div>

        {/* Mensagem Sequencial em Destaque */}
        <div className="mt-6 text-center space-y-1">
          <h3 className="text-base sm:text-lg font-bold text-slate-800 transition-all duration-300">
            {etapaAtual.mensagem}
          </h3>
          <p className="text-xs text-slate-600 max-w-md mx-auto">
            {etapaAtual.descricao}
          </p>
        </div>
      </div>

      {/* Barra de Progresso Animada */}
      <div className="max-w-xl mx-auto space-y-2">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-600 px-1">
          <span>Progresso da Execução</span>
          <span className="text-[#FF5800]">{porcentagemProgresso}%</span>
        </div>

        <div className="w-full h-2.5 rounded-full bg-slate-100 overflow-hidden shadow-inner p-0.5 border border-slate-200">
          <div
            className="h-full rounded-full bg-gradient-to-r from-[#FF5800] to-[#FF7A00] transition-all duration-500 ease-out shadow-xs"
            style={{ width: `${porcentagemProgresso}%` }}
          />
        </div>
      </div>

      {/* Checklist das 4 Microetapas em Execução */}
      <div className="max-w-2xl mx-auto grid grid-cols-1 sm:grid-cols-2 gap-3 pt-4 border-t border-slate-100">
        {ETAPAS.map((etapa, idx) => {
          const isPassado = idx < etapaAtualIndex;
          const isPresente = idx === etapaAtualIndex;

          return (
            <div
              key={etapa.id}
              className={`flex items-center gap-3 p-3 rounded-xl border transition-all duration-200 ${
                isPresente
                  ? 'bg-[#FFF7ED] border-[#FFEDD5] text-[#9A3412] shadow-2xs'
                  : isPassado
                  ? 'bg-slate-50 border-slate-200 text-slate-800'
                  : 'bg-white border-slate-100 text-slate-400 opacity-60'
              }`}
            >
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 text-xs font-bold ${
                  isPassado
                    ? 'bg-emerald-100 text-emerald-700'
                    : isPresente
                    ? 'bg-[#FF5800] text-white shadow-2xs'
                    : 'bg-slate-100 text-slate-400'
                }`}
              >
                {isPassado ? <CheckCircle2 size={16} /> : idx + 1}
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold truncate">
                  {etapa.mensagem.replace('...', '')}
                </p>
                <p className="text-[10px] text-slate-600 truncate">
                  {idx === 0 && 'OpenCV Image Filter'}
                  {idx === 1 && 'Tesseract Engine'}
                  {idx === 2 && 'Spring AI LLM Parser'}
                  {idx === 3 && 'Business Rule Engine'}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Card Informativo do Arquivo em Análise */}
      {arquivo && (
        <div className="max-w-md mx-auto flex items-center justify-center gap-3 py-2.5 px-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600">
          <FileText size={16} className="text-[#FF5800] flex-shrink-0" />
          <span className="font-semibold text-slate-700 truncate max-w-[200px]">
            {arquivo.nome}
          </span>
          <span className="text-slate-300">•</span>
          <span>{arquivo.tamanhoFormatado}</span>
        </div>
      )}
    </div>
  );
};
