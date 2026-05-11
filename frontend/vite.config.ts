import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  base: '/agente-flutuante-ia-app/',

  define: {
    global: 'globalThis',
  },

  plugins: [react()],

  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9998',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:9998',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})