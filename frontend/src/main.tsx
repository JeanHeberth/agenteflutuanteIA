import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

if (typeof window !== 'undefined' && !("global" in window)) {
  ;(window as Window & typeof globalThis & { global?: typeof globalThis }).global = globalThis
}

const root = createRoot(document.getElementById('root')!)

import('./App.tsx').then(({ default: App }) => {
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
})
