import React from 'react';
import {
  ArrowLeft,
  Calendar,
  FileText,
  Send,
  CheckCircle2,
  Users,
  HelpCircle,
  ChevronRight,
  LucideIcon,
} from 'lucide-react';
import { Avatar } from '../common/Avatar';
import { NavItemId } from '../../types/navigation';

interface SidebarProps {
  activeItem?: NavItemId;
  onSelectItem?: (id: NavItemId) => void;
  onBack?: () => void;
  isOpenMobile?: boolean;
  onCloseMobile?: () => void;
}

interface SidebarLink {
  id: NavItemId;
  label: string;
  icon: LucideIcon;
  badge?: string;
  badgeType?: 'orange' | 'neutral';
}

const NAV_LINKS: SidebarLink[] = [
  {
    id: 'encontro-2',
    label: 'Encontro 2',
    icon: Calendar,
  },
  {
    id: 'documentos',
    label: 'Documentos',
    icon: FileText,
    badge: 'Ativo',
    badgeType: 'orange',
  },
  {
    id: 'meus-envios',
    label: 'Meus envios',
    icon: Send,
  },
  {
    id: 'resultados',
    label: 'Resultados',
    icon: CheckCircle2,
  },
  {
    id: 'equipe',
    label: 'Equipe',
    icon: Users,
  },
  {
    id: 'ajuda',
    label: 'Ajuda',
    icon: HelpCircle,
  },
];

export const Sidebar: React.FC<SidebarProps> = ({
  activeItem = 'documentos',
  onSelectItem,
  onBack,
  isOpenMobile = false,
  onCloseMobile,
}) => {
  const handleItemClick = (id: NavItemId) => {
    if (onSelectItem) {
      onSelectItem(id);
    }
    if (onCloseMobile) {
      onCloseMobile();
    }
  };

  const handleBackClick = () => {
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  return (
    <>
      {/* Overlay escuro para Mobile */}
      {isOpenMobile && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-xs lg:hidden transition-opacity"
          onClick={onCloseMobile}
          aria-hidden="true"
        />
      )}

      {/* Sidebar container */}
      <aside
        className={`fixed top-16 bottom-0 left-0 z-40 w-64 bg-white border-r border-slate-200 flex flex-col justify-between transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          isOpenMobile ? 'translate-x-0 shadow-2xl' : '-translate-x-full'
        }`}
      >
        <div className="flex flex-col flex-1 overflow-y-auto px-4 py-5 space-y-6">
          
          {/* Botão Superior "← Voltar" */}
          <button
            type="button"
            onClick={handleBackClick}
            className="group flex items-center gap-2.5 px-3 py-2 text-sm font-semibold text-slate-600 hover:text-[#FF5800] bg-slate-50 hover:bg-[#FFF7ED] rounded-xl border border-slate-200 hover:border-[#FFEDD5] transition-all shadow-2xs"
            title="Voltar para a página anterior"
          >
            <ArrowLeft
              size={18}
              className="text-slate-500 group-hover:text-[#FF5800] transition-transform group-hover:-translate-x-0.5"
            />
            <span>Voltar</span>
          </button>

          {/* Seção de Navegação Principal */}
          <nav className="space-y-1.5" aria-label="Navegação Principal">
            <p className="px-3 text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-2">
              Menu de Acesso
            </p>

            {NAV_LINKS.map((item) => {
              const Icon = item.icon;
              const isActive = activeItem === item.id;

              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => handleItemClick(item.id)}
                  className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 select-none ${
                    isActive
                      ? 'bg-[#FFF7ED] text-[#FF5800] font-semibold border-l-4 border-[#FF5800] shadow-2xs'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80'
                  }`}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <div className="flex items-center gap-3">
                    <Icon
                      size={19}
                      className={isActive ? 'text-[#FF5800]' : 'text-slate-500'}
                    />
                    <span>{item.label}</span>
                  </div>

                  {/* Badges contextuais */}
                  {item.badge && isActive ? (
                    <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-[#FFEDD5] text-[#9A3412]">
                      {item.badge}
                    </span>
                  ) : isActive ? (
                    <ChevronRight size={16} className="text-[#FF5800]" />
                  ) : null}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Rodapé da Sidebar: Card do Usuário */}
        <div className="p-4 border-t border-slate-200 bg-slate-50/60">
          <div className="flex items-center gap-3 p-2.5 rounded-xl bg-white border border-slate-200 shadow-2xs hover:border-slate-300 transition-colors">
            <Avatar initials="KM" name="Kleber Meira" size="md" showOnlineBadge={true} />
            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-sm font-semibold text-slate-800 truncate">
                Kleber Meira
              </span>
              <span className="text-xs font-medium text-slate-600 truncate flex items-center gap-1.5">
                <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500" />
                Grupo A
              </span>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};

