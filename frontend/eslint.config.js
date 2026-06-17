import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

const deepRelativeImportPatterns = [
  '../../*',
  '../../../*',
  '../../../../*',
  '../../../../../*',
  '../../../../../../*',
]

const layerImportRule = (restrictedLayers) => [
  'error',
  {
    patterns: [
      {
        group: deepRelativeImportPatterns,
        message: 'Use layer aliases such as @shared, @entities, @features, @widgets, or @pages instead of deep relative imports.',
      },
      ...restrictedLayers.map((layer) => ({
        group: [`${layer}/*`],
        message: `This layer must not import from ${layer}. Follow app -> pages -> widgets -> features -> entities -> shared.`,
      })),
    ],
  },
]

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
    },
  },
  {
    files: ['src/shared/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app', '@pages', '@widgets', '@features', '@modules', '@entities']),
    },
  },
  {
    files: ['src/entities/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app', '@pages', '@widgets', '@features', '@modules']),
    },
  },
  {
    files: ['src/features/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app', '@pages', '@widgets']),
    },
  },
  {
    files: ['src/widgets/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app', '@pages']),
    },
  },
  {
    files: ['src/pages/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app']),
    },
  },
])
