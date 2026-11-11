import React from 'react';
import {
  CheckCircle2,
  XCircle,
  Download,
  RotateCcw,
  User,
  CreditCard,
  MapPin,
  Calendar,
  Building2,
  FileText,
  AlertTriangle,
  Info,
} from 'lucide-react';
import { DocumentoRespostaDTO } from '../../types/document';
import { downloadRelatorioJson } from '../../services/documentoService';

interface ResultViewProps {
  resultado: DocumentoRespostaDTO;
  onReset: () => void;
}

export const ResultView: React.FC<ResultViewProps> = ({
  resultado,
  onReset,
}) => {
  const isAprovado = resultado.status === 'APROVADO';

  const criterios = [
    {
      label: 'CPF encontrado e válido',
      atendido:
        Boolean(resultado.criterios?.cpfEncontrado) &&
        Boolean(resultado.criterios?.cpfValido),
      detalhes: resultado.criterios?.cpfValido
        ? 'Dígitos verificadores calculados com sucesso'
        : 'CPF inválido ou não identificado no documento',
    },
    {
      label: 'Endereço identificado',
      atendido: Boolean(resultado.criterios?.enderecoEncontrado),
      detalhes: resultado.criterios?.enderecoEncontrado
        ? 'Logradouro e número reconhecidos'
        : 'Endereço residencial não encontrado',
    },
    {
      label: 'Documento legível',
      atendido: Boolean(resultado.criterios?.documentoLegivel),
      detalhes: resultado.criterios?.documentoLegivel
        ? 'Resolução e nitidez aprovadas pelo OpenCV'
        : 'Imagem borrada, com corte ou ilegível',
    },
    {
      label: 'Emissão < 90 dias',
      atendido: Boolean(resultado.criterios?.emissaoMenor90Dias),
      detalhes: resultado.criterios?.emissaoMenor90Dias
        ? 'Comprovante recente dentro do período de validade'
        : 'Documento expedido há mais de 90 dias ou data ilegível',
    },
  ];

  const handleDownload = () => {
    downloadRelatorioJson(
      resultado,
      `relatorio_${resultado.nomeArquivo || 'validacao'}`
    );
  };

  const formatarData = (dataStr?: string | null) => {
    if (!dataStr) return 'Não informada';
    try {
      const [ano, mes, dia] = dataStr.split('-');
      if (ano && mes && dia) {
        return `${dia}/${mes}/${ano}`;
      }
      return dataStr;
    } catch {
      return dataStr;
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-2xs space-y-8 animate-in fade-in-50 duration-300">
      
      {/* 1. Header com Status Geral e Identificador */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
        <div>
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Parecer de Validação Documental
          </span>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight mt-0.5">
            Resultado da Análise
          </h2>
          <p className="text-xs text-slate-500 mt-1 flex items-center gap-2">
            <span>Arquivo: <strong>{resultado.nomeArquivo}</strong></span>
            <span>•</span>
            <span>ID: <code className="bg-slate-100 px-1 py-0.5 rounded text-[11px] font-mono">{resultado.documentoId}</code></span>
          </p>
        </div>

        {/* Data e hora de processamento */}
        <div className="text-right hidden sm:block">
          <span className="text-[11px] text-slate-500 font-medium block">
            Processado em
          </span>
          <span className="text-xs font-semibold text-slate-700">
            {resultado.dataProcessamento
              ? new Date(resultado.dataProcessamento).toLocaleString('pt-BR')
              : new Date().toLocaleString('pt-BR')}
          </span>
        </div>
      </div>

      {/* 2. Badge de Status Grande (APROVADO / REPROVADO) */}
      <div
        className={`p-6 sm:p-8 rounded-2xl border flex flex-col sm:flex-row items-center sm:items-start gap-5 transition-all ${
          isAprovado
            ? 'bg-emerald-50/70 border-emerald-200 text-emerald-950'
            : 'bg-red-50/70 border-red-200 text-red-950'
        }`}
      >
        <div
          className={`w-16 h-16 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-sm ${
            isAprovado
              ? 'bg-emerald-500 text-white ring-4 ring-emerald-100'
              : 'bg-red-500 text-white ring-4 ring-red-100'
          }`}
        >
          {isAprovado ? (
            <CheckCircle2 size={36} strokeWidth={2.5} />
          ) : (
            <XCircle size={36} strokeWidth={2.5} />
          )}
        </div>

        <div className="space-y-1.5 text-center sm:text-left flex-1">
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
            <span
              className={`px-3.5 py-1 rounded-full text-xs font-black tracking-wider uppercase shadow-2xs ${
                isAprovado
                  ? 'bg-emerald-600 text-white'
                  : 'bg-red-600 text-white'
              }`}
            >
              {isAprovado ? '🟢 APROVADO' : '🔴 REPROVADO'}
            </span>
            <span className="text-xs font-semibold opacity-75">
              Tipo: {resultado.tipoDocumento}
            </span>
          </div>

          <h3 className="text-lg sm:text-xl font-bold">
            {isAprovado
              ? 'Documento em conformidade com as regras de validação'
              : 'Documento reprovado nas regras de validação'}
          </h3>

          <p className="text-xs sm:text-sm opacity-90 leading-relaxed">
            {resultado.mensagem ||
              (isAprovado
                ? 'Todos os critérios de legibilidade, identificação do titular e data de emissão foram cumpridos com sucesso.'
                : 'O documento não atende aos requisitos mínimos exigidos.')}
          </p>

          {/* Motivo detalhado de reprovação se houver */}
          {!isAprovado && resultado.motivoReprovacao && (
            <div className="mt-3 p-3 rounded-xl bg-red-100/80 border border-red-200 text-xs text-red-900 flex items-start gap-2 text-left">
              <AlertTriangle size={16} className="text-red-700 flex-shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Motivo: </span>
                <span>{resultado.motivoReprovacao}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 3. Grid: Checklist dos 4 Critérios de Validação + Dados Extraídos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Coluna 1: Checklist dos 4 Critérios */}
        <div className="bg-slate-50/70 rounded-2xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-200/80">
            <div className="flex items-center gap-2">
              <div className="p-1 rounded-md bg-orange-100 text-[#FF5800]">
                <FileText size={16} />
              </div>
              <h4 className="text-sm font-bold text-slate-800">
                Critérios de Validação (4 Fases)
              </h4>
            </div>
            <span className="text-xs font-semibold text-slate-500">
              {criterios.filter((c) => c.atendido).length} de {criterios.length} atendidos
            </span>
          </div>

          <div className="space-y-3">
            {criterios.map((criterio, index) => (
              <div
                key={index}
                className={`p-3.5 rounded-xl border flex items-start gap-3 transition-colors ${
                  criterio.atendido
                    ? 'bg-white border-emerald-200 text-slate-800'
                    : 'bg-white border-red-200 text-slate-800'
                }`}
              >
                <div
                  className={`p-1 rounded-full mt-0.5 flex-shrink-0 ${
                    criterio.atendido
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-red-100 text-red-700'
                  }`}
                >
                  {criterio.atendido ? (
                    <CheckCircle2 size={16} />
                  ) : (
                    <XCircle size={16} />
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-bold text-slate-800">
                      {criterio.label}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        criterio.atendido
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {criterio.atendido ? 'OK' : 'Falhou'}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 mt-0.5 leading-snug">
                    {criterio.detalhes}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Coluna 2: Card / Tabela de Dados Extraídos */}
        <div className="bg-slate-50/70 rounded-2xl border border-slate-200 p-5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-200/80">
            <div className="flex items-center gap-2">
              <div className="p-1 rounded-md bg-orange-100 text-[#FF5800]">
                <User size={16} />
              </div>
              <h4 className="text-sm font-bold text-slate-800">
                Dados Extraídos (OCR + Spring AI)
              </h4>
            </div>
            <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-100">
              Estruturado
            </span>
          </div>

          <div className="divide-y divide-slate-200/80 bg-white rounded-xl border border-slate-200 overflow-hidden">
            {/* Nome */}
            <div className="p-3 flex items-start gap-3">
              <User size={16} className="text-slate-400 mt-0.5 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide block">
                  Nome Completo
                </span>
                <span className="text-xs font-bold text-slate-800 break-words">
                  {resultado.dadosExtraidos?.nome || (
                    <em className="text-slate-400 font-normal">Não identificado</em>
                  )}
                </span>
              </div>
            </div>

            {/* CPF */}
            <div className="p-3 flex items-start gap-3">
              <CreditCard size={16} className="text-slate-400 mt-0.5 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide block">
                  CPF
                </span>
                <span className="text-xs font-bold text-slate-800 font-mono">
                  {resultado.dadosExtraidos?.cpf || (
                    <em className="text-slate-400 font-normal font-sans">Não identificado</em>
                  )}
                </span>
              </div>
            </div>

            {/* Endereço */}
            <div className="p-3 flex items-start gap-3">
              <MapPin size={16} className="text-slate-400 mt-0.5 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide block">
                  Endereço Residencial
                </span>
                <span className="text-xs font-semibold text-slate-800 break-words">
                  {resultado.dadosExtraidos?.endereco || (
                    <em className="text-slate-400 font-normal">Não identificado</em>
                  )}
                </span>
              </div>
            </div>

            {/* Cidade / UF */}
            <div className="p-3 flex items-start gap-3">
              <Building2 size={16} className="text-slate-400 mt-0.5 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide block">
                  Município / UF
                </span>
                <span className="text-xs font-semibold text-slate-800">
                  {resultado.dadosExtraidos?.cidade || resultado.dadosExtraidos?.estado ? (
                    `${resultado.dadosExtraidos?.cidade || '—'} - ${resultado.dadosExtraidos?.estado || '—'}`
                  ) : (
                    <em className="text-slate-400 font-normal">Não identificado</em>
                  )}
                </span>
              </div>
            </div>

            {/* Data de Emissão */}
            <div className="p-3 flex items-start gap-3">
              <Calendar size={16} className="text-slate-400 mt-0.5 flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide block">
                  Data de Emissão do Documento
                </span>
                <span className="text-xs font-semibold text-slate-800">
                  {formatarData(resultado.dadosExtraidos?.dataEmissao)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 4. Ações Finais: Baixar Relatório JSON e Validar Novo Documento */}
      <div className="pt-6 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-4">
        
        {/* Botão Baixar Relatório JSON */}
        <button
          type="button"
          onClick={handleDownload}
          className="w-full sm:w-auto px-5 py-3 rounded-xl border border-slate-300 hover:border-[#FF5800] bg-white hover:bg-[#FFF7ED]/50 text-slate-700 hover:text-[#FF5800] font-bold text-xs uppercase tracking-wider transition-all duration-200 flex items-center justify-center gap-2.5 shadow-2xs cursor-pointer group"
          title="Fazer download do arquivo JSON completo com os resultados"
        >
          <Download size={16} className="group-hover:translate-y-0.5 transition-transform" />
          <span>Baixar Relatório JSON</span>
        </button>

        {/* Botão Validar Novo Documento */}
        <button
          type="button"
          onClick={onReset}
          className="w-full sm:w-auto px-7 py-3 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white font-bold text-xs uppercase tracking-wider transition-all duration-200 flex items-center justify-center gap-2.5 shadow-2xs hover:shadow-orange-glow active:scale-98 cursor-pointer"
        >
          <RotateCcw size={16} />
          <span>Validar Novo Documento</span>
        </button>
      </div>

      {/* Aviso de Auditoria e Conformidade LGPD */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-500 flex items-center gap-2">
        <Info size={15} className="text-slate-400 flex-shrink-0" />
        <span>
          Os registros de auditoria foram persistidos de forma segura no PostgreSQL sob as normas da LGPD e diretrizes de residência tecnológica da Avanade.
        </span>
      </div>
    </div>
  );
};

