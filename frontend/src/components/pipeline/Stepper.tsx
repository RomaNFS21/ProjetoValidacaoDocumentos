import React from 'react';
import { Check } from 'lucide-react';
import { StepFase, PIPELINE_STEPS } from '../../types/document';

interface StepperProps {
  currentStep: StepFase;
  onStepClick?: (step: StepFase) => void;
  className?: string;
}

export const Stepper: React.FC<StepperProps> = ({
  currentStep = 1,
  onStepClick,
  className = '',
}) => {
  return (
    <nav
      aria-label="Progresso do Pipeline"
      className={`w-full bg-white rounded-xl border border-slate-200 p-4 sm:p-6 shadow-2xs ${className}`}
    >
      <ol className="flex items-center justify-between w-full relative">
        {PIPELINE_STEPS.map((step, index) => {
          const isCompleted = step.fase < currentStep;
          const isCurrent = step.fase === currentStep;
          const isLast = index === PIPELINE_STEPS.length - 1;

          return (
            <li
              key={step.fase}
              className={`relative flex-1 ${!isLast ? 'pr-4 sm:pr-8' : ''}`}
            >
              <div className="flex items-center">
                {/* Linha Conectora com a próxima etapa */}
                {!isLast && (
                  <div
                    className="absolute top-5 left-10 sm:left-12 right-0 -mr-2 sm:-mr-4 h-0.5 -translate-y-1/2 z-0 transition-colors duration-300"
                    style={{
                      backgroundColor: isCompleted ? '#22C55E' : '#E2E8F0',
                    }}
                    aria-hidden="true"
                  />
                )}

                {/* Botão / Nó da Etapa */}
                <button
                  type="button"
                  onClick={() => onStepClick && onStepClick(step.fase)}
                  disabled={!onStepClick || (!isCompleted && !isCurrent)}
                  className={`relative z-10 flex items-center gap-3 group text-left transition-all ${
                    onStepClick && (isCompleted || isCurrent)
                      ? 'cursor-pointer'
                      : 'cursor-default'
                  }`}
                  aria-current={isCurrent ? 'step' : undefined}
                >
                  {/* Círculo do Indicador */}
                  <div
                    className={`w-9 h-9 sm:w-10 sm:h-10 rounded-full flex items-center justify-center font-bold text-xs sm:text-sm transition-all duration-300 shadow-2xs ${
                      isCompleted
                        ? 'bg-emerald-500 text-white ring-4 ring-emerald-50'
                        : isCurrent
                        ? 'bg-[#FF5800] text-white ring-4 ring-[#FFF7ED] shadow-[0_0_12px_rgba(255,88,0,0.35)]'
                        : 'bg-slate-100 text-slate-400 border border-slate-200'
                    }`}
                  >
                    {isCompleted ? (
                      <Check size={18} strokeWidth={2.5} className="animate-in zoom-in-50 duration-200" />
                    ) : (
                      <span>{step.fase}</span>
                    )}
                  </div>

                  {/* Informações Textuais da Etapa */}
                  <div className="hidden md:flex flex-col select-none">
                    <span
                      className={`text-xs font-bold uppercase tracking-wider transition-colors ${
                        isCurrent
                          ? 'text-[#FF5800]'
                          : isCompleted
                          ? 'text-slate-800'
                          : 'text-slate-600'
                      }`}
                    >
                      {step.titulo}
                    </span>
                    <span
                      className={`text-[11px] leading-tight ${
                        isCurrent
                          ? 'text-slate-700 font-medium'
                          : isCompleted
                          ? 'text-slate-500'
                          : 'text-slate-600'
                      }`}
                    >
                      {step.subtitulo}
                    </span>
                  </div>
                </button>
              </div>

              {/* Rótulo simplificado abaixo do nó para dispositivos móveis */}
              <div className="md:hidden mt-2 select-none">
                <span
                  className={`block text-[11px] font-bold truncate ${
                    isCurrent
                      ? 'text-[#FF5800]'
                      : isCompleted
                      ? 'text-slate-700'
                      : 'text-slate-600'
                  }`}
                >
                  {step.titulo}
                </span>
              </div>
            </li>
          );
        })}
      </ol>
    </nav>
  );
};

