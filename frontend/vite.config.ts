import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1', port: 5173, allowedHosts: process.env.RANGER_ALLOW_ALL_DEV_HOSTS === 'true' ? true : [],
    proxy: { '/api': { target: process.env.RANGER_DEV_BACKEND || 'http://127.0.0.1:8080', changeOrigin: true } },
  },
})
