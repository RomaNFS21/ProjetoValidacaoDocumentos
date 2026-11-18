import React from 'react';
import { Users, Award, Shield, Mail } from 'lucide-react';
import { Avatar } from '../common/Avatar';

export const EquipeView: React.FC = () => {
  const membros = [
    {
      nome: 'Victor Roma',
      iniciais: 'VR',
      papel: 'Desenvolvedor',
      grupo: 'Cesar',
      email: 'victor.roma@aluno.cesar.school',
      status: 'Desenvolvedor do Projeto',
    },
    {
      nome: 'Kleber Meira',
      iniciais: 'KM',
      papel: 'Mentoria',
      grupo: 'Avanade',
      email: 'kleber.meira@avanade.com',
      status: 'Mentor Especialista',
    },
    {
      nome: 'Residência Tecnológica',
      iniciais: 'RT',
      papel: 'Programa de Capacitação',
      grupo: 'Porto Digital & CESAR',
      email: 'residencia@portodigital.org',
      status: 'Turma 2026.2',
    },
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-2xs space-y-8 animate-in fade-in-50 duration-300">
      <div className="border-b border-slate-100 pb-5">
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2.5">
          <Users size={24} className="text-[#FF5800]" />
          <span>Equipe do Projeto</span>
        </h2>
        <p className="text-xs sm:text-sm text-slate-500 mt-1">
          Participantes da Mentoria 2026.2 Recife — Residência Tecnológica Porto Digital / Avanade.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {membros.map((membro, index) => (
          <div
            key={index}
            className="p-5 rounded-2xl border border-slate-200 bg-slate-50/60 hover:bg-white hover:border-[#FFEDD5] transition-all shadow-2xs space-y-4"
          >
            <div className="flex items-center gap-3.5">
              <Avatar initials={membro.iniciais} name={membro.nome} size="lg" />
              <div>
                <h4 className="text-sm font-bold text-slate-900">{membro.nome}</h4>
                <p className="text-xs text-slate-500">{membro.papel}</p>
                <span className="inline-block mt-1 px-2 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-[#FF5800]">
                  {membro.grupo}
                </span>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-200/80 space-y-1.5 text-xs text-slate-600">
              <div className="flex items-center gap-2">
                <Shield size={14} className="text-emerald-600" />
                <span>{membro.status}</span>
              </div>
              <div className="flex items-center gap-2 text-slate-400">
                <Mail size={14} />
                <span className="truncate">{membro.email}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center gap-3 text-xs text-slate-600">
        <Award size={20} className="text-[#FF5800] flex-shrink-0" />
        <span>
          Projeto desenvolvido como parte do Desafio Prático de Validação Automatizada de Documentos utilizando Inteligência Artificial e Engenharia de Software.
        </span>
      </div>
    </div>
  );
};

