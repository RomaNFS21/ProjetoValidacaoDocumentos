import React from 'react';
import {
  Droplets,
  Zap,
  Phone,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
  Lightbulb,
  FileCheck,
} from 'lucide-react';

interface SupportPanelProps {
  className?: string;
}

export const SupportPanel: React.FC<SupportPanelProps> = ({ className = '' }) => {
  const comprovantes = [
    {
      icon: Droplets,
      nome: 'Conta de água',
      descricao: 'Saneamento / Abastecimento',
      corIcone: 'text-sky-500 bg-sky-50 border-sky-100',
    },
    {
      icon: Zap,
      nome: 'Conta de luz',
      descricao: 'Distribuidora de energia',
      corIcone: 'text-amber-500 bg-amber-50 border-amber-100',
    },
    {
      icon: Phone,
      nome: 'Telefone / Internet',
      descricao: 'Fatura de serviços fixos/móveis',
      corIcone: 'text-indigo-500 bg-indigo-50 border-indigo-100',
    },
    {
      icon: CreditCard,
      nome: 'Fatura de cartão',
      descricao: 'Bancária ou loja de crédito',
      corIcone: 'text-emerald-500 bg-emerald-50 border-emerald-100',
    },
  ];

  const dicas = [
    'Documento bem enquadrado e sem cortes nas bordas',
    'Imagem nítida, com boa iluminação e texto legível',
    'Evite reflexos de flash, brilhos e sombras fortes',
    'Formatos aceitos: PDF, PNG ou JPEG até 10MB',
  ];

  return (
    <aside
      className={`w-full xl:w-80 2xl:w-96 flex flex-col gap-5 ${className}`}
      aria-label="Painel de Apoio e Orientações"
    >
      {/* Card 1: Exemplos de comprovantes */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs hover:shadow-sm transition-shadow">
        <div className="flex items-center gap-2.5 mb-4 pb-2 border-b border-slate-100">
          <div className="p-1.5 rounded-lg bg-orange-50 text-[#FF5800]">
            <FileCheck size={18} />
          </div>
          <h3 className="font-bold text-sm text-slate-800">
            Exemplos de comprovantes
          </h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-1 gap-2.5">
          {comprovantes.map((item, index) => {
            const Icon = item.icon;
            return (
              <div
                key={index}
                className="flex items-center gap-3 p-2.5 rounded-lg border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition-colors"
              >
                <div
                  className={`w-9 h-9 rounded-lg flex items-center justify-center border flex-shrink-0 ${item.corIcone}`}
                >
                  <Icon size={18} />
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-slate-800 truncate">
                    {item.nome}
                  </p>
                  <p className="text-[11px] text-slate-600 truncate">
                    {item.descricao}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Card 2: Dicas para um melhor resultado */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-2xs hover:shadow-sm transition-shadow">
        <div className="flex items-center gap-2.5 mb-3.5 pb-2 border-b border-slate-100">
          <div className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">
            <Lightbulb size={18} />
          </div>
          <h3 className="font-bold text-sm text-slate-800">
            Dicas para um melhor resultado
          </h3>
        </div>

        <ul className="space-y-2.5">
          {dicas.map((dica, index) => (
            <li key={index} className="flex items-start gap-2.5 text-xs text-slate-600 leading-relaxed">
              <CheckCircle2
                size={16}
                className="text-[#22C55E] flex-shrink-0 mt-0.5"
              />
              <span>{dica}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Card 3: Alerta "Importante" (Caixa Pastel Avanade) */}
      <div className="bg-[#FFF7ED] border border-[#FFEDD5] rounded-xl p-4.5 shadow-2xs">
        <div className="flex items-start gap-3">
          <div className="p-1.5 rounded-lg bg-[#FFEDD5] text-[#FF5800] flex-shrink-0 mt-0.5">
            <AlertTriangle size={18} />
          </div>
          <div className="space-y-1">
            <h4 className="text-xs font-bold text-[#9A3412] uppercase tracking-wide">
              Importante
            </h4>
            <p className="text-xs text-[#9A3412]/90 leading-relaxed font-normal">
              O documento deve ter sido emitido nos <strong>últimos 90 dias</strong> e estar em nome do titular ou de parente de 1º grau devidamente comprovado.
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
};

