import React, { useState } from 'react';
import { HelpCircle, ChevronDown, ChevronUp, CheckCircle, AlertTriangle } from 'lucide-react';

export const AjudaView: React.FC = () => {
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const faqs = [
    {
      pergunta: 'Quais tipos de documentos são aceitos como comprovante de residência?',
      resposta:
        'São aceitas contas de consumo público essenciais: água (ex: Compesa), energia elétrica (ex: Neoenergia/Celpe), gás canalizado, telefone fixo/móvel, internet ou faturas de cartão de crédito recentes.',
    },
    {
      pergunta: 'Qual é o prazo limite para a data de emissão do documento?',
      resposta:
        'Para comprovantes de residência, o documento deve ter sido emitido nos últimos 90 dias. Para documentos de identificação civil (RG e CNH), a emissão deve ser de no máximo 10 anos a contar da data de expedição.',
    },
    {
      pergunta: 'Quais os formatos de arquivo e tamanhos permitidos?',
      resposta:
        'A plataforma aceita arquivos nos formatos PDF (.pdf), PNG (.png) ou JPEG/JPG (.jpg, .jpeg) de até 10MB.',
    },
    {
      pergunta: 'O comprovante pode estar em nome de terceiros?',
      resposta:
        'O comprovante deve preferencialmente estar no nome do titular do cadastro. Caso esteja em nome de pais ou cônjuge, é necessário apresentar certidão ou comprovante de parentesco de 1º grau.',
    },
    {
      pergunta: 'Como funciona o pipeline de validação automatizada?',
      resposta:
        'O arquivo passa por 4 fases: (1) Upload seguro; (2) Filtros OpenCV para alinhamento e redução de ruído; (3) Tesseract OCR e extração estruturada de dados com Spring AI; (4) Validação algorítmica de CPF e regras temporais.',
    },
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-2xs space-y-8 animate-in fade-in-50 duration-300">
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2.5">
          <HelpCircle size={24} className="text-[#FF5800]" />
          <span>Central de Ajuda & Dúvidas Frequentes</span>
        </h2>
        <p className="text-xs sm:text-sm text-slate-500 mt-1">
          Orientações completas sobre requisitos de envio, regras de aprovação e funcionamento da plataforma.
        </p>
      </div>

      {/* Accordion FAQ */}
      <div className="space-y-3">
        {faqs.map((faq, index) => {
          const isOpen = openFaq === index;
          return (
            <div
              key={index}
              className="rounded-xl border border-slate-200 overflow-hidden transition-all"
            >
              <button
                type="button"
                onClick={() => setOpenFaq(isOpen ? null : index)}
                className="w-full p-4 text-left flex items-center justify-between gap-4 bg-slate-50/60 hover:bg-slate-50 font-semibold text-xs sm:text-sm text-slate-800 transition-colors cursor-pointer"
              >
                <span>{faq.pergunta}</span>
                {isOpen ? (
                  <ChevronUp size={16} className="text-[#FF5800] flex-shrink-0" />
                ) : (
                  <ChevronDown size={16} className="text-slate-400 flex-shrink-0" />
                )}
              </button>

              {isOpen && (
                <div className="p-4 bg-white border-t border-slate-100 text-xs text-slate-600 leading-relaxed animate-in fade-in-50 duration-150">
                  {faq.resposta}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Dicas de Sucesso */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 space-y-1.5 text-xs text-emerald-900">
          <div className="flex items-center gap-2 font-bold">
            <CheckCircle size={16} className="text-emerald-600" />
            <span>Boas Práticas de Envio</span>
          </div>
          <p className="leading-relaxed opacity-90">
            Fotografe em ambiente bem iluminado, sem sombras cobrindo o cabeçalho e mantendo os 4 cantos da folha visíveis.
          </p>
        </div>

        <div className="p-4 rounded-xl bg-[#FFF7ED] border border-[#FFEDD5] space-y-1.5 text-xs text-[#9A3412]">
          <div className="flex items-center gap-2 font-bold">
            <AlertTriangle size={16} className="text-[#FF5800]" />
            <span>Evite Reprovações</span>
          </div>
          <p className="leading-relaxed opacity-90">
            Não envie comprovantes recortados, telas de celular fotografadas ou documentos com dados cadastrais ilegíveis.
          </p>
        </div>
      </div>
    </div>
  );
};

