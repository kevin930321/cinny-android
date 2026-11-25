import { CinnyThemeMode } from '../lib/cinnyEngine';

export interface PhoneThemeClasses {
  mode: CinnyThemeMode;
  isDark: boolean;
  frameBorder: string;
  bg: string;
  surface: string;
  surfaceVar: string;
  line: string;
  divider: string;
  text: string;
  muted: string;
  primaryBg: string;
  primaryText: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  success: string;
  warning: string;
  critical: string;
  hoverCritical: string;
  checkboxAccent: string;
  hoverSurface: string;
}

const LIGHT_MXID_CLASSES = [
  'text-[#007AE6]',
  'text-[#990094]',
  'text-[#00996E]',
  'text-[#E60041]',
  'text-[#E65C00]',
  'text-[#009699]',
  'text-[#0600E6]',
  'text-[#4EB300]',
];

const DARK_MXID_CLASSES = [
  'text-[#80C4FF]',
  'text-[#FF99FB]',
  'text-[#66FFD4]',
  'text-[#FF80A4]',
  'text-[#FFA266]',
  'text-[#33FAFF]',
  'text-[#9D99FF]',
  'text-[#BBFF99]',
];

export function getMxidTextClass(userId: string, isDark: boolean): string {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = (hash << 5) - hash + userId.charCodeAt(i);
    hash |= 0;
  }
  const idx = Math.abs(hash) % 8;
  return isDark ? DARK_MXID_CLASSES[idx] : LIGHT_MXID_CLASSES[idx];
}

export function getPhoneThemeClasses(mode: CinnyThemeMode): PhoneThemeClasses {
  switch (mode) {
    case 'Light':
      return {
        mode,
        isDark: false,
        frameBorder: 'border-slate-900',
        bg: 'bg-[#F5F5F5]',
        surface: 'bg-[#FFFFFF]',
        surfaceVar: 'bg-[#EAEAEA]',
        line: 'border-[#E0E0E0]',
        divider: 'bg-[#E0E0E0]',
        text: 'text-[#0E0E0E]',
        muted: 'text-[#525252]',
        primaryBg: 'bg-[#1245A8]',
        primaryText: 'text-[#1245A8]',
        onPrimary: 'text-[#FFFFFF]',
        primaryContainer: 'bg-[#C4D0E9]',
        onPrimaryContainer: 'text-[#0D3076]',
        success: 'text-[#017343]',
        warning: 'text-[#864300]',
        critical: 'text-[#9D0F0F]',
        hoverCritical: 'hover:text-[#9D0F0F]',
        checkboxAccent: 'accent-[#1245A8]',
        hoverSurface: 'hover:bg-[#EAEAEA]',
      };
    case 'Silver':
      return {
        mode,
        isDark: false,
        frameBorder: 'border-slate-900',
        bg: 'bg-[#DEDEDE]',
        surface: 'bg-[#EAEAEA]',
        surfaceVar: 'bg-[#D2D2D2]',
        line: 'border-[#C7C7C7]',
        divider: 'bg-[#C7C7C7]',
        text: 'text-[#0C0C0C]',
        muted: 'text-[#474747]',
        primaryBg: 'bg-[#1245A8]',
        primaryText: 'text-[#1245A8]',
        onPrimary: 'text-[#FFFFFF]',
        primaryContainer: 'bg-[#C4D0E9]',
        onPrimaryContainer: 'text-[#0D3076]',
        success: 'text-[#017343]',
        warning: 'text-[#864300]',
        critical: 'text-[#9D0F0F]',
        hoverCritical: 'hover:text-[#9D0F0F]',
        checkboxAccent: 'accent-[#1245A8]',
        hoverSurface: 'hover:bg-[#D2D2D2]',
      };
    case 'Butter':
      return {
        mode,
        isDark: true,
        frameBorder: 'border-[#3C3B34]',
        bg: 'bg-[#1A1916]',
        surface: 'bg-[#262621]',
        surfaceVar: 'bg-[#33322C]',
        line: 'border-[#3C3B34]',
        divider: 'bg-[#3C3B34]',
        text: 'text-[#FFFBDE]',
        muted: 'text-[#ACA78D]',
        primaryBg: 'bg-[#BDB6EC]',
        primaryText: 'text-[#BDB6EC]',
        onPrimary: 'text-[#2C2843]',
        primaryContainer: 'bg-[#413C65]',
        onPrimaryContainer: 'text-[#E3E1F7]',
        success: 'text-[#85E0BA]',
        warning: 'text-[#E3BA91]',
        critical: 'text-[#E69D9D]',
        hoverCritical: 'hover:text-[#E69D9D]',
        checkboxAccent: 'accent-[#BDB6EC]',
        hoverSurface: 'hover:bg-[#33322C]',
      };
    case 'Dark':
    default:
      return {
        mode: 'Dark',
        isDark: true,
        frameBorder: 'border-[#3A3A3A]',
        bg: 'bg-[#1A1A1A]',
        surface: 'bg-[#262626]',
        surfaceVar: 'bg-[#333333]',
        line: 'border-[#3A3A3A]',
        divider: 'bg-[#3A3A3A]',
        text: 'text-[#F2F2F2]',
        muted: 'text-[#A6A6A6]',
        primaryBg: 'bg-[#BDB6EC]',
        primaryText: 'text-[#BDB6EC]',
        onPrimary: 'text-[#2C2843]',
        primaryContainer: 'bg-[#413C65]',
        onPrimaryContainer: 'text-[#E3E1F7]',
        success: 'text-[#85E0BA]',
        warning: 'text-[#E3BA91]',
        critical: 'text-[#E69D9D]',
        hoverCritical: 'hover:text-[#E69D9D]',
        checkboxAccent: 'accent-[#BDB6EC]',
        hoverSurface: 'hover:bg-[#333333]',
      };
  }
}
