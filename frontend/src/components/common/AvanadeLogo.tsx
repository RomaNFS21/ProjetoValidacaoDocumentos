import React from 'react';

interface AvanadeLogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
}

export const AvanadeLogo: React.FC<AvanadeLogoProps> = ({
  className = '',
  size = 'md',
  showText = true,
}) => {
  const heightClasses = {
    sm: 'h-6',
    md: 'h-8',
    lg: 'h-10',
  };

  return (
    <div className={`flex items-center gap-2.5 select-none ${className}`}>
      {/* Símbolo Icônico Avanade (Arco / Degradê Laranja) */}
      <svg
        viewBox="0 0 40 40"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={`${heightClasses[size]} w-auto aspect-square flex-shrink-0 transition-transform duration-300 hover:scale-105`}
      >
        <defs>
          <linearGradient id="avanadeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FF5800" />
            <stop offset="100%" stopColor="#FF3000" />
          </linearGradient>
          <linearGradient id="avanadeAccent" x1="0%" y1="100%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#FF7A00" />
            <stop offset="100%" stopColor="#FF4500" />
          </linearGradient>
        </defs>

        {/* Fundo Arredondado Moderno */}
        <rect width="40" height="40" rx="10" fill="url(#avanadeGradient)" />

        {/* Ícone Estilizado da Marca */}
        <path
          d="M12 28L20 12L28 28H23L20 21L17 28H12Z"
          fill="white"
          fillOpacity="0.95"
        />
        <circle cx="20" cy="24" r="2.5" fill="white" />
        <path
          d="M8 20C8 13.3726 13.3726 8 20 8"
          stroke="rgba(255,255,255,0.4)"
          strokeWidth="2.5"
          strokeLinecap="round"
        />
      </svg>

      {/* Tipografia Avanade */}
      {showText && (
        <div className="flex flex-col">
          <span className="font-extrabold tracking-tight text-xl text-slate-900 leading-none flex items-center">
            avanade
            <span className="w-1.5 h-1.5 rounded-full bg-[#FF5800] ml-0.5 inline-block"></span>
          </span>
          <span className="text-[10px] uppercase tracking-wider font-semibold text-slate-600 mt-0.5">
            Mentoria Tech
          </span>
        </div>
      )}
    </div>
  );
};

