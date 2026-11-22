import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
// The weight axis of IBM Plex Sans, served with the web; a visitor downloads the subset of the
// characters the page uses, Latin for Spanish and English (ADR 0017).
import '@fontsource-variable/ibm-plex-sans/wght.css'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
