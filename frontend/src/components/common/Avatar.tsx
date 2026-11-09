import React from 'react';

interface AvatarProps {
  initials?: string;
  name?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  showOnlineBadge?: boolean;
}

export const Avatar: React.FC<AvatarProps> = ({
  initials = 'KM',
  name = 'Kleber Meira',
  size = 'md',
  className = '',
  showOnlineBadge = true,
}) => {
  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-12 h-12 text-base',
  };

  const badgeSizeClasses = {
    sm: 'w-2 h-2',
    md: 'w-2.5 h-2.5',
    lg: 'w-3 h-3',
  };

  return (
    <div className={`relative inline-block ${className}`} title={name}>
      <div
        className={`${sizeClasses[size]} rounded-full bg-gradient-to-tr from-[#FF4500] to-[#FF7A00] text-white font-bold flex items-center justify-center shadow-sm ring-2 ring-white/80 transition-transform duration-200 hover:scale-105 select-none`}
      >
        {initials}
      </div>

      {showOnlineBadge && (
        <span
          className={`absolute bottom-0 right-0 ${badgeSizeClasses[size]} bg-emerald-500 border-2 border-white rounded-full ring-1 ring-emerald-400/30`}
          aria-label="Online"
        />
      )}
    </div>
  );
};

