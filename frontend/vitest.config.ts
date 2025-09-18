import { defineConfig } from 'vitest/config'
import { resolve } from 'path'

export default defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    globals: true,
    css: false, // Disable CSS processing to avoid PostCSS issues
    include: ['__tests__/**/*.{test,spec}.{js,ts,tsx}'], // Only include unit tests
    exclude: ['tests/**/*', 'node_modules/**/*'], // Exclude E2E tests
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'tests/',
        '*.config.*',
        '*.d.ts',
        'coverage/',
        'dist/',
        '.next/',
      ],
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './'),
    },
  },
})