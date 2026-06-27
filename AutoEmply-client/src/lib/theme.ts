/** Shared dark-theme color palette and style objects. */

export const colors = {
  /** App shell main background. */
  appBackground: '#0b0e12',
  /** Header, navbar, drawer and input surfaces. */
  surface: '#0f1318',
  /** Panel (PageSection) background. */
  panelBackground: '#11151b',
  /** Inline alert background. */
  alertBackground: '#141920',
  /** Muted button background. */
  controlBackground: '#151a21',
  /** Selected table-row background. */
  rowSelectedBackground: '#171d24',
  /** Active nav-item background. */
  navActiveBackground: '#1a2028',
  /** Header/navbar divider border. */
  surfaceBorder: '#232830',
  /** Panel border. */
  panelBorder: '#2a2f38',
  /** Input, drawer and nav-item border. */
  controlBorder: '#2b313a',
  /** Muted button border. */
  buttonBorder: '#2f353e',
  /** Primary foreground text. */
  textPrimary: '#d6dbe3',
  /** Secondary description text. */
  textSecondary: '#8b93a1',
  /** Uppercase form-label text. */
  textLabel: '#87909d',
  /** Muted caption text. */
  textMuted: '#7f8794',
  /** Faintest section-label text. */
  textFaint: '#6f7785',
} as const

export const monoFontFamily = 'JetBrains Mono, D2Coding, Consolas, monospace'

export const mutedButtonStyles = {
  root: {
    border: `1px solid ${colors.buttonBorder}`,
    background: colors.controlBackground,
    color: colors.textPrimary,
  },
  label: {
    color: colors.textPrimary,
  },
}

export const panelStyle = {
  border: `1px solid ${colors.panelBorder}`,
  background: colors.panelBackground,
  boxShadow: 'none',
}

export const monoLabelStyles = {
  label: {
    fontFamily: monoFontFamily,
    fontSize: '0.74rem',
    letterSpacing: '0.08em',
    textTransform: 'uppercase' as const,
    color: colors.textLabel,
  },
  input: {
    background: colors.surface,
    borderColor: colors.controlBorder,
    color: colors.textPrimary,
    fontFamily: monoFontFamily,
  },
}

export const alertStyles = {
  root: {
    background: colors.alertBackground,
    borderColor: colors.controlBorder,
    color: colors.textPrimary,
  },
}
