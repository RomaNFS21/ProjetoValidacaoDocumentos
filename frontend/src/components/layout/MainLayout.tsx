import React, { useState } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { SupportPanel } from './SupportPanel';
import { NavItemId } from '../../types/navigation';

interface MainLayoutProps {
  children?: React.ReactNode;
  activeNav?: NavItemId;
  onNavigate?: (id: NavItemId) => void;
  showSupportPanel?: boolean;
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  activeNav = 'documentos',
  onNavigate,
  showSupportPanel = true,
}) => {
  const [currentNav, setCurrentNav] = useState<NavItemId>(activeNav);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  const handleNavSelect = (id: NavItemId) => {
    setCurrentNav(id);
    if (onNavigate) {
      onNavigate(id);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-slate-800 flex flex-col font-sans antialiased selection:bg-[#FFF7ED] selection:text-[#FF5800]">
      {/* Header Superior Fixo */}
      <Header
        onToggleSidebar={() => setIsMobileSidebarOpen(!isMobileSidebarOpen)}
        isSidebarOpen={isMobileSidebarOpen}
      />

      {/* Container Principal abaixo da Header */}
      <div className="flex-1 flex w-full">
        {/* Sidebar Esquerda */}
        <Sidebar
          activeItem={currentNav}
          onSelectItem={handleNavSelect}
          isOpenMobile={isMobileSidebarOpen}
          onCloseMobile={() => setIsMobileSidebarOpen(false)}
        />

        {/* Área Central de Conteúdo + Painel Lateral de Apoio */}
        <main className="flex-1 lg:pl-64 flex flex-col min-w-0 transition-all duration-300">
          <div className="max-w-[1720px] w-full mx-auto p-4 sm:p-6 lg:p-8 flex flex-col xl:flex-row gap-6 lg:gap-8 items-start">
            
            {/* Conteúdo Principal (Upload, Processamento, Resultados) */}
            <section className="flex-1 min-w-0 w-full space-y-6">
              {children}
            </section>

            {/* Painel Lateral Direito de Apoio (Exemplos, Dicas, Alertas) */}
            {showSupportPanel && (
              <div className="w-full xl:w-auto flex-shrink-0">
                <SupportPanel />
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
};

