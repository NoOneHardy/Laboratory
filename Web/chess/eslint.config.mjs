import eslint from '@eslint/js'
import tsEsLint from 'typescript-eslint'
import stylistic from '@stylistic/eslint-plugin'

export default tsEsLint.config(
  eslint.configs.recommended,
  tsEsLint.configs.recommended,
  tsEsLint.configs.stylistic,
  {
    plugins: {
      '@stylistic': stylistic
    },
    rules: {
      'no-unused-vars': 'error',
      'no-var': 'error',
      'no-undef': 'off',
      '@typescript-eslint/no-inferrable-types': 'off',
      '@stylistic/indent': ['error', 2],
      '@stylistic/semi': ['error', 'never'],
      '@stylistic/quotes': ['error', 'single']
    }
  }
)