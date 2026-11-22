/**
 * The delta of the favicon as an inline mark, in the brand colour of the current scheme. It is
 * decorative: the text next to it names the application.
 */
export function BrandMark({ size = 24 }: { size?: number }) {
  return (
    <svg viewBox="0 0 64 64" width={size} height={size} aria-hidden="true" focusable="false">
      <rect width="64" height="64" rx="13" fill="var(--delfos-palette-primary-main)" />
      <path d="M32 13 13 51h38z" fill="var(--delfos-palette-primary-contrastText)" />
      <path d="M32 24.5 21.5 46h21z" fill="var(--delfos-palette-primary-main)" />
    </svg>
  )
}
