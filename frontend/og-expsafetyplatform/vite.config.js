import { fileURLToPath, URL } from 'node:url'

import { createLogger, defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const logger = createLogger()
const originalWarn = logger.warn
logger.warn = (message, options) => {
  if (message.includes('[INVALID_ANNOTATION]') && message.includes('@vueuse/core')) {
    return
  }
  originalWarn(message, options)
}

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  customLogger: logger,
  plugins: [
    vue(),
    command === 'serve' ? vueDevTools() : null,
  ].filter(Boolean),
  server: {
    proxy: {
      '/api':{
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) return 'vue'
          if (id.includes('/@element-plus/icons-vue/')) return 'element-icons'
          const elementComponent = id.match(/\/element-plus\/es\/components\/([^/]+)\//)
          if (elementComponent) return `ep-${elementComponent[1]}`
          if (id.includes('/element-plus/es/')) return 'element-core'
          if (id.includes('/@vueuse/')) return 'vueuse'
          if (id.includes('/echarts/')) return 'charts'
          return 'vendor'
        },
      },
    },
  },
}))
