import { useEffect } from 'react'

const APP_NAME = 'Delfos'

/** Names the browser tab after the page, so history, bookmarks and screen readers tell pages apart. */
export function usePageTitle(title: string | null | undefined) {
  useEffect(() => {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME
    return () => {
      document.title = APP_NAME
    }
  }, [title])
}
