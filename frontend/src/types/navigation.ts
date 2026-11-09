export type NavItemId =
  | 'encontro-2'
  | 'documentos'
  | 'meus-envios'
  | 'resultados'
  | 'equipe'
  | 'ajuda';

export interface NavItem {
  id: NavItemId;
  label: string;
  iconName: string;
  badge?: string;
  href?: string;
}

export interface UserProfile {
  name: string;
  role: string;
  group: string;
  initials: string;
  avatarUrl?: string;
}

