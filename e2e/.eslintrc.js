module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: [
    '@typescript-eslint',
    'jest',
    'prefer-arrow',
    'simple-import-sort',
  ],
  extends: [
    'eslint:recommended',
    'plugin:prettier/recommended'
  ],
  env: {
    node: true,
    jest: true,
    es6: true,
    browser: true
  },
  globals: {
    page: true,
    browser: true,
    context: true,
    jestPuppeteer: true,
    globalThis: "readonly"
  },
  parserOptions: {
    project: './tsconfig.json',
    ecmaVersion: 2018,
    sourceType: 'module'
  },
  ignorePatterns: ['tsc-out', 'node_modules', '.eslintrc.js'],
  rules: {
    /* Style */
    'prettier/prettier': ['warn', {singleQuote: true, tabWidth: 2}],
    'max-len': [
      'warn', 120,
      {
        ignorePattern: '^import |^export\\{(.*?)\\}',
        ignoreComments: true,
        ignoreUrls: true,
        tabWidth: 2,
        ignoreRegExpLiterals: true
      }],

    /* Code Quality */
    'no-constant-condition': 'off',
    'quotes': ['warn', 'single', 'avoid-escape'],
    'quote-props': ['warn', 'consistent-as-needed'],
    'eqeqeq': ['warn', 'always', { null: 'ignore' }],
    'guard-for-in': 'warn',
    'no-bitwise': 'warn',
    'no-caller': 'warn',
    'no-debugger': 'warn',
    'no-empty': 'warn',
    'no-eval': 'warn',
    'no-fallthrough': 'warn', // For switch statements
    'no-new-wrappers': 'warn',
    'semi': ['warn', 'always'],
    'no-duplicate-imports': 'warn',
    'no-restricted-imports': ['warn', { paths: ['rxjs'], patterns: ['../'] }],
    'no-throw-literal': 'warn',
    'no-undef-init': 'warn',
    'no-var': 'warn',
    'prefer-const': ['warn', { destructuring: 'all' }],
    '@typescript-eslint/no-use-before-define': ['warn', { "functions": false }],
    'no-unused-vars': 'off', // Needed for the below rule
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '_' }],
    '@typescript-eslint/consistent-type-definitions': ['warn', 'interface'],
    '@typescript-eslint/dot-notation': 'warn',
    '@typescript-eslint/no-inferrable-types': ['warn', { ignoreParameters: true },],
    '@typescript-eslint/no-misused-new': 'warn',
    '@typescript-eslint/no-non-null-assertion': 'warn',
    '@typescript-eslint/no-shadow': 'warn',
    '@typescript-eslint/prefer-optional-chain': 'warn',
    '@typescript-eslint/unified-signatures': 'warn',
    '@typescript-eslint/require-await': 'warn',
    '@typescript-eslint/no-floating-promises': 'warn',

    'comma-dangle': 'error',
    'arrow-parens': 'error',
    'eol-last': ['error', 'always'],
    'space-before-function-paren': [
      'warn',
      {
        anonymous: 'always',
        named: 'never',
        asyncArrow: 'always'
    }],

    /* Jest */
    'jest/no-focused-tests': 'warn',
  }
};
