import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Shared configuration for both frontend apps
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': '/src',
    },
  },
})
