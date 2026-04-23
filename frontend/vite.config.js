import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

const chunkPackages = {
  core: ['vue', 'vue-router', 'pinia', 'vue-i18n', 'vue-demi', '@vue/devtools-api', 'lodash-unified'],
  element: ['element-plus', '@element-plus/icons-vue'],
  content: ['marked', 'dompurify', 'md-editor-v3']
}

const getPackageName = (id) => {
  const packagePath = id.split('node_modules/')[1]
  if (!packagePath) {
    return null
  }

  const parts = packagePath.split('/')
  if (parts[0].startsWith('@')) {
    return `${parts[0]}/${parts[1]}`
  }

  return parts[0]
}

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router'],
      resolvers: [ElementPlusResolver()],
      dts: false
    }),
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: 'css'
        })
      ],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          const packageName = getPackageName(id)
          if (!packageName) {
            return 'vendor'
          }

          if (chunkPackages.core.includes(packageName)) {
            return 'vendor-core'
          }

          if (chunkPackages.element.includes(packageName)) {
            return 'vendor-element'
          }

          if (chunkPackages.content.includes(packageName)) {
            return 'vendor-content'
          }

          return `vendor-${packageName.replace('@', '').replace('/', '-')}`
        }
      }
    }
  },
  server: {
    port: 5173,
    host: true,
    allowedHosts: ['blog.ayakacloud.cn'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
        xfwd: true
      }
    }
  }
})
