import eslint from '@eslint/js'
import tslint from 'typescript-eslint'
import vuePlugin from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'
import globals from 'globals'
import path from 'path'
import {fileURLToPath} from 'url'
import {globalIgnores} from 'eslint/config'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

export default tslint.config(
  globalIgnores(['node_modules/**', 'dist/**']),
  eslint.configs.recommended,
  tslint.configs.recommendedTypeChecked,
  tslint.configs.stylistic,
  vuePlugin.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: '@typescript-eslint/parser',
        extraFileExtensions: ['vue'],
        projectService: true,
        tsconfigRootDir: __dirname
      },
      globals: {
        ...globals.browser
      }
    },
    rules: {
      quotes: ['error', 'single'],
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/html-self-closing': 'off'
    }
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tslint.parser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: __dirname
      }
    },
    rules: {
      quotes: ['error', 'single']
    }
  }
)
