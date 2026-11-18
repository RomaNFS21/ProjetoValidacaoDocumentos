import React, { useState } from 'react';
import {
  FileText,
  CheckCircle2,
  XCircle,
  Clock,
  Search,
  Filter,
  Download,
  Eye,
} from 'lucide-react';
import { DocumentoRespostaDTO, StatusValidacao } from '../../types/document';
import { downloadRelatorioJson } from '../../services/documentoService';

interface MeusEnviosViewProps {
  onVerDetalhes: (item: DocumentoRespostaDTO) => void;
  onNovoEnvio: () => void;
}

const MOCK_ENVIOS: DocumentoRespostaDTO[] = [
  {
    documentoId: '8f92a10b-1234-4567-89ab-cdef01234567',
    nomeArquivo: 'comprovante_celpe_victor.pdf',
    tipoDocumento: 'COMPROVANTE_RESIDENCIA',
    status: 'APROVADO',
    mensagem: 'Documento validado com sucesso. Todos os critérios foram atendidos.',
    criterios: {
      cpfEncontrado: true,
      cpfValido: true,
      enderecoEncontrado: true,
      documentoLegivel: true,
      emissaoMenor90Dias: true,
    },
    dadosExtraidos: {
      nome: 'Victor Roma',
      cpf: '123.456.789-00',
      endereco: 'Rua do Bom Jesus, 180, Recife Antigo',
      cidade: 'Recife',
      estado: 'PE',
      dataEmissao: '2026-10-14',
    },
    dataProcessamento: '2026-10-21T14:32:00',
  },
  {
    documentoId: '3c4d5e6f-7890-4123-8456-7890abcdef12',
    nomeArquivo: 'fatura_cartao_antiga.jpg',
    tipoDocumento: 'COMPROVANTE_RESIDENCIA',
    status: 'REPROVADO',
    mensagem: 'Documento reprovado: data de emissão ultrapassa o limite máximo de 90 dias.',
    motivoReprovacao: 'Comprovante emitido há mais de 120 dias.',
    criterios: {
      cpfEncontrado: true,
      cpfValido: true,
      enderecoEncontrado: true,
      documentoLegivel: true,
      emissaoMenor90Dias: false,
    },
    dadosExtraidos: {
      nome: 'Victor Roma',
      cpf: '123.456.789-00',
      endereco: 'Av. Conde da Boa Vista, 400',
      cidade: 'Recife',
      estado: 'PE',
      dataEmissao: '2026-05-10',
    },
    dataProcessamento: '2026-10-20T11:15:22',
  },
  {
    documentoId: '5a6b7c8d-9012-4345-9678-9012345678ab',
    nomeArquivo: 'conta_agua_compesa.png',
    tipoDocumento: 'COMPROVANTE_RESIDENCIA',
    status: 'APROVADO',
    mensagem: 'Documento validado com sucesso.',
    criterios: {
      cpfEncontrado: true,
      cpfValido: true,
      enderecoEncontrado: true,
      documentoLegivel: true,
      emissaoMenor90Dias: true,
    },
    dadosExtraidos: {
      nome: 'Victor Roma',
      cpf: '123.456.789-00',
      endereco: 'Rua do Sol, 55, Santo Antônio',
      cidade: 'Recife',
      estado: 'PE',
      dataEmissao: '2026-10-01',
    },
    dataProcessamento: '2026-10-18T09:40:11',
  },
];

export const MeusEnviosView: React.FC<MeusEnviosViewProps> = ({
  onVerDetalhes,
  onNovoEnvio,
}) => {
  const [filtroStatus, setFiltroStatus] = useState<'TODOS' | StatusValidacao>('TODOS');
  const [termoBusca, setTermoBusca] = useState('');

  const enviosFiltrados = MOCK_ENVIOS.filter((item) => {
    const matchStatus = filtroStatus === 'TODOS' || item.status === filtroStatus;
    const matchBusca =
      item.nomeArquivo.toLowerCase().includes(termoBusca.toLowerCase()) ||
      item.dadosExtraidos?.nome?.toLowerCase().includes(termoBusca.toLowerCase()) ||
      item.documentoId.toLowerCase().includes(termoBusca.toLowerCase());
    return matchStatus && matchBusca;
  });

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-2xs space-y-6 animate-in fade-in-50 duration-300">
      
      {/* Cabeçalho */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Meus Envios
          </h2>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Histórico completo de validações de comprovantes submetidos ao pipeline.
          </p>
        </div>

        <button
          type="button"
          onClick={onNovoEnvio}
          className="px-5 py-2.5 rounded-xl bg-[#FF5800] hover:bg-[#FF4500] text-white text-xs font-bold uppercase tracking-wider transition-all shadow-2xs hover:shadow-orange-glow cursor-pointer self-start sm:self-auto"
        >
          + Enviar Novo Documento
        </button>
      </div>

      {/* Barra de Filtro e Busca */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Campo de Busca */}
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Buscar por nome do arquivo, titular ou ID..."
            value={termoBusca}
            onChange={(e) => setTermoBusca(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#FF5800]"
          />
        </div>

        {/* Filtros de Status */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl text-xs font-medium text-slate-600">
          <Filter size={14} className="ml-2 text-slate-400 hidden sm:block" />
          {(['TODOS', 'APROVADO', 'REPROVADO'] as const).map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => setFiltroStatus(status)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                filtroStatus === status
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'hover:text-slate-900'
              }`}
            >
              {status === 'TODOS'
                ? 'Todos'
                : status === 'APROVADO'
                ? 'Aprovados'
                : 'Reprovados'}
            </button>
          ))}
        </div>
      </div>

      {/* Tabela de Envios */}
      <div className="border border-slate-200 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3 px-4">Documento</th>
                <th className="py-3 px-4">Titular Extraído</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Processado em</th>
                <th className="py-3 px-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {enviosFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    Nenhum documento encontrado com os filtros selecionados.
                  </td>
                </tr>
              ) : (
                enviosFiltrados.map((item) => {
                  const isAprovado = item.status === 'APROVADO';
                  return (
                    <tr
                      key={item.documentoId}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2.5">
                          <div className="p-1.5 rounded-lg bg-orange-50 text-[#FF5800]">
                            <FileText size={16} />
                          </div>
                          <div>
                            <span className="font-bold text-slate-800 block truncate max-w-[200px]" title={item.nomeArquivo}>
                              {item.nomeArquivo}
                            </span>
                            <span className="text-[10px] text-slate-400 font-mono">
                              ID: {item.documentoId.substring(0, 8)}...
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 font-medium text-slate-700">
                        {item.dadosExtraidos?.nome || '—'}
                      </td>

                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold border ${
                            isAprovado
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : 'bg-red-50 text-red-700 border-red-200'
                          }`}
                        >
                          {isAprovado ? (
                            <CheckCircle2 size={13} className="text-emerald-600" />
                          ) : (
                            <XCircle size={13} className="text-red-600" />
                          )}
                          {isAprovado ? 'Aprovado' : 'Reprovado'}
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-slate-500">
                        <div className="flex items-center gap-1.5">
                          <Clock size={13} className="text-slate-400" />
                          <span>
                            {new Date(item.dataProcessamento).toLocaleString('pt-BR')}
                          </span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => onVerDetalhes(item)}
                            className="p-1.5 rounded-lg border border-slate-200 hover:border-[#FF5800] text-slate-600 hover:text-[#FF5800] hover:bg-[#FFF7ED] transition-colors cursor-pointer"
                            title="Ver detalhes da validação"
                          >
                            <Eye size={15} />
                          </button>

                          <button
                            type="button"
                            onClick={() => downloadRelatorioJson(item)}
                            className="p-1.5 rounded-lg border border-slate-200 hover:border-slate-400 text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors cursor-pointer"
                            title="Baixar JSON"
                          >
                            <Download size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

