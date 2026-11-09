import React, { useState } from 'react';
import { Bell, Menu, X } from 'lucide-react';
import { AvanadeLogo } from '../common/AvanadeLogo';
import { Avatar } from '../common/Avatar';

interface HeaderProps {
  onToggleSidebar?: () => void;
  isSidebarOpen?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  onToggleSidebar,
  isSidebarOpen = false,
}) => {
  const [notificationCount] = useState<number>(3);

  return (
    <header className="sticky top-0 z-40 w-full bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-sm transition-all">
      <div className="max-w-[1720px] mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        
        {/* Esquerda: Botão mobile + Logo Avanade */}
        <div className="flex items-center gap-3">
          {onToggleSidebar && (
            <button
              type="button"
              onClick={onToggleSidebar}
              className="lg:hidden p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-[#FF5800]"
              aria-label={isSidebarOpen ? 'Fechar menu lateral' : 'Abrir menu lateral'}
            >
              {isSidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          )}

          <a href="#" className="flex items-center focus:outline-none focus:ring-2 focus:ring-[#FF5800] rounded-lg">
            <AvanadeLogo size="md" showText={true} />
          </a>
        </div>

        {/* Centro: Texto da Mentoria */}
        <div className="hidden md:flex items-center justify-center">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-50 border border-slate-200/80 shadow-[inset_0_1px_2px_rgba(0,0,0,0.02)]">
            <span className="w-2 h-2 rounded-full bg-[#FF5800] animate-pulse" />
            <span className="text-xs sm:text-sm font-semibold tracking-wider text-slate-800 uppercase">
              MENTORIA 2026.2 | RECIFE - RESIDÊNCIA TECNOLÓGICA
            </span>
          </div>
        </div>

        {/* Direita: Notificações + Avatar */}
        <div className="flex items-center gap-4">
          {/* Badge para dispositivos móveis exibindo a mentoria */}
          <div className="md:hidden flex items-center">
            <span className="text-[10px] font-bold text-slate-600 uppercase tracking-tight bg-slate-100 px-2 py-0.5 rounded">
              Recife 2026.2
            </span>
          </div>

          {/* Botão de Notificações com Badge */}
          <button
            type="button"
            className="relative p-2 rounded-full text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-[#FF5800]"
            aria-label="Notificações"
            title="Ver notificações"
          >
            <Bell size={20} className="text-slate-600" />
            {notificationCount > 0 && (
              <span className="absolute top-1 right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-[#FF5800] rounded-full ring-2 ring-white">
                {notificationCount}
              </span>
            )}
          </button>

          {/* Divisor vertical sutil */}
          <div className="h-6 w-px bg-slate-200 hidden sm:block" />

          {/* Perfil / Avatar KM */}
          <div className="flex items-center gap-3 pl-1">
            <Avatar initials="KM" name="Kleber Meira" size="md" showOnlineBadge={true} />
            <div className="hidden xl:flex flex-col text-left select-none">
              <span className="text-sm font-semibold text-slate-800 leading-tight">
                Kleber Meira
              </span>
              <span className="text-xs text-slate-500">
                Grupo A
              </span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

