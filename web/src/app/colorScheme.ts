/**
 * Where the visitor's choice of colour scheme lives and how the document carries it, shared by
 * the theme, the application shell and the script public/lang.js runs before the first paint.
 */
export const MODE_STORAGE_KEY = 'delfos.mode'

/** The attribute of the root element that selects a colour scheme: `light` or `dark`. */
export const SCHEME_ATTRIBUTE = 'data-delfos-scheme'

export type Mode = 'light' | 'dark' | 'system'

export const MODES: readonly Mode[] = ['system', 'light', 'dark']
