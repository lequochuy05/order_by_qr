import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'
import eslintConfigPrettier from 'eslint-config-prettier'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const listSlices = (layer) => {
  const dir = path.resolve(__dirname, 'src', layer)
  if (!fs.existsSync(dir)) return []
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort()
}

const entitySlices = listSlices('entities')
const featureSlices = listSlices('features')
const widgetSlices = listSlices('widgets')

const deepRelativeImportPatterns = [
  '../../*',
  '../../../*',
  '../../../../*',
  '../../../../../*',
  '../../../../../../*',
]

const externalDeepPatterns = (allowedOwnPrefix = null) => {
  const patterns = [
    {
      group: ['@entities/*/*', '@features/*/*', '@widgets/*/*'],
      message: 'Import from slice Public API barrel (e.g. @entities/order, @features/auth, @widgets/admin-layout) instead of internal paths.',
    },
  ]
  if (allowedOwnPrefix) {
    // Allow intra-slice alias imports (e.g. @features/settings-management/lib/*) by explicitly negating own slice
    return [
      {
        group: [
          '@entities/*/*',
          '@features/*/*',
          '@widgets/*/*',
          `!${allowedOwnPrefix}/*`,
          `!${allowedOwnPrefix}/**`,
        ],
        message: 'Import external slices via their Public API barrel (index.js) instead of internal paths.',
      },
    ]
  }
  return patterns
}

const layerImportRule = (restrictedLayers, extraPatterns = [], ownSlicePrefix = null) => [
  'error',
  {
    patterns: [
      {
        group: deepRelativeImportPatterns,
        message: 'Use layer aliases such as @shared, @entities, @features, @widgets, or @pages instead of deep relative imports.',
      },
      ...restrictedLayers.map((layer) => ({
        group: [layer, `${layer}/*`],
        message: `This layer must not import from ${layer}. Follow app -> pages -> widgets -> features -> entities -> shared.`,
      })),
      ...externalDeepPatterns(ownSlicePrefix),
      ...extraPatterns,
    ],
  },
]

const entitySliceRules = entitySlices.map((slice) => {
  const otherEntities = entitySlices.filter((other) => other !== slice).map((other) => `@entities/${other}`)
  return {
    files: [`src/entities/${slice}/**/*.{js,jsx}`],
    rules: {
      'no-restricted-imports': layerImportRule(
        ['@app', '@pages', '@widgets', '@features'],
        otherEntities.length > 0
          ? [
              {
                group: otherEntities.flatMap((prefix) => [prefix, `${prefix}/*`]),
                message: `Entity "${slice}" must not import from another entity slice.`,
              },
            ]
          : [],
        `@entities/${slice}`,
      ),
    },
  }
})

const featureSliceRules = featureSlices.map((slice) => {
  const otherFeatures = featureSlices.filter((other) => other !== slice).map((other) => `@features/${other}`)
  return {
    files: [`src/features/${slice}/**/*.{js,jsx}`],
    rules: {
      'no-restricted-imports': layerImportRule(
        ['@app', '@pages', '@widgets'],
        otherFeatures.length > 0
          ? [
              {
                group: otherFeatures.flatMap((prefix) => [prefix, `${prefix}/*`]),
                message: `Feature "${slice}" must not import from another feature slice.`,
              },
            ]
          : [],
        `@features/${slice}`,
      ),
    },
  }
})

const widgetSliceRules = widgetSlices.map((slice) => ({
  files: [`src/widgets/${slice}/**/*.{js,jsx}`],
  rules: {
    'no-restricted-imports': layerImportRule(['@app', '@pages'], [], `@widgets/${slice}`),
  },
}))

export default defineConfig([
  globalIgnores(['dist', 'dev-dist', 'android']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      eslintConfigPrettier,
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
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]', argsIgnorePattern: '^[A-Z_]' }],
    },
  },
  {
    files: ['src/shared/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app', '@pages', '@widgets', '@features', '@entities']),
    },
  },
  ...entitySliceRules,
  ...featureSliceRules,
  ...widgetSliceRules,
  {
    files: ['src/pages/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule(['@app']),
    },
  },
  {
    files: ['src/app/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': layerImportRule([]),
    },
  },
])
