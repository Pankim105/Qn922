import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import dts from 'vite-plugin-dts'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isLibrary = mode === 'library'
  
  if (isLibrary) {
    return {
      plugins: [
        react(),
        dts({
          insertTypesEntry: true,
          outDir: 'dist',
          include: ['src/**/*'],
          exclude: ['src/**/*.test.*', 'src/**/*.spec.*', 'src/demo.tsx'],
          copyDtsFiles: true,
          staticImport: true,
        }),
      ],
      resolve: {
        alias: {
          '@': resolve(__dirname, './src'),
        },
      },
      build: {
        lib: {
          entry: resolve(__dirname, 'src/index.ts'),
          name: 'ModernUI',
          formats: ['es', 'cjs'],
          fileName: (format) => `index.${format === 'es' ? 'esm' : 'cjs'}.js`,
        },
        rollupOptions: {
          external: [
            'react', 
            'react-dom', 
            'react/jsx-runtime',
            '@radix-ui/react-dialog',
            '@radix-ui/react-slot',
            'class-variance-authority',
            'clsx',
            'lucide-react',
            'tailwind-merge'
          ],
          output: {
            globals: {
              react: 'React',
              'react-dom': 'ReactDOM',
              'react/jsx-runtime': 'react/jsx-runtime',
            },
            assetFileNames: (assetInfo) => {
              if (assetInfo.name === 'style.css') {
                return 'style.css'
              }
              return assetInfo.name
            },
          },
        },
        sourcemap: true,
        minify: false,
        cssCodeSplit: false,
      },
    }
  }
  
  // 开发模式配置
  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
      },
    },
    test: {
      environment: 'jsdom',
      globals: true,
    },
  }
})
