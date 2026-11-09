/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        avanade: {
          50: '#FFF7ED',
          100: '#FFEDD5',
          200: '#FED7AA',
          300: '#FDBA74',
          400: '#FB923C',
          500: '#FF5800', // Laranja oficial Avanade
          600: '#FF4500', // Hover primário
          700: '#C23600',
          800: '#9A2E00',
          900: '#7C2600',
          DEFAULT: '#FF5800',
          hover: '#FF4500',
        },
        background: {
          DEFAULT: '#F8FAFC',
          subtle: '#F1F5F9',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          subtle: '#F8FAFC',
          muted: '#F1F5F9',
        },
        state: {
          success: {
            DEFAULT: '#22C55E',
            light: '#F0FDF4',
            border: '#BBF7D0',
          },
          error: {
            DEFAULT: '#EF4444',
            light: '#FEF2F2',
            border: '#FECACA',
          },
          warning: {
            DEFAULT: '#F59E0B',
            light: '#FFFBEB',
            border: '#FDE68A',
          },
          alert: {
            DEFAULT: '#FF5800',
            bg: '#FFF7ED',
            border: '#FFEDD5',
            text: '#9A3412',
          },
        },
      },
      fontFamily: {
        sans: ['Inter', 'Roboto', 'system-ui', '-apple-system', 'sans-serif'],
      },
      borderRadius: {
        'xl': '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      boxShadow: {
        'soft': '0 2px 10px -2px rgba(15, 23, 42, 0.05), 0 1px 3px -1px rgba(15, 23, 42, 0.03)',
        'card': '0 4px 20px -2px rgba(15, 23, 42, 0.06), 0 2px 6px -1px rgba(15, 23, 42, 0.04)',
        'orange-glow': '0 4px 14px 0 rgba(255, 88, 0, 0.25)',
      },
    },
  },
  plugins: [],
};

