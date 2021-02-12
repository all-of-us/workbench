module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  // TODO Add 'prettier/@typescript-eslint', 'plugin:prettier/recommended' in extends array.
  extends: ['plugin:@typescript-eslint/recommended'],
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
    jestPuppeteer: true
  },
  parserOptions: {
    project: './tsconfig.json',
    ecmaVersion: 2018,
    sourceType: 'module'
  },
  ignorePatterns: ['tsc-out', 'node_modules', '.eslintrc.js'],
  rules: {
    // 2 == error, 1 == warning, 0 == off
    'max-len': [
      2,
      140,
      {
        ignoreComments: true,
        ignoreUrls: true,
        tabWidth: 2,
        ignoreRegExpLiterals: true
      }
    ],
    'no-implicit-coercion': [
      2,
      {
        boolean: false,
        number: true,
        string: true
      }
    ],
    'no-unused-expressions': [
      1,
      {
        allowShortCircuit: true,
        allowTernary: false
      }
    ],
    // TODO Fix warnings, then change to ERROR level.
    '@typescript-eslint/restrict-template-expressions': 1,
    '@typescript-eslint/no-non-null-assertion': 1,
    '@typescript-eslint/no-unsafe-member-access': 1,
    '@typescript-eslint/no-unsafe-assignment': 1,
    '@typescript-eslint/no-unsafe-call': 1,
    '@typescript-eslint/no-unsafe-return': 1,
    '@typescript-eslint/no-var-requires': 1,
    '@typescript-eslint/no-inferrable-types': 1,
    '@typescript-eslint/await-thenable': 1,
    '@typescript-eslint/no-misused-promises': 1,
    '@typescript-eslint/unbound-method': 1,
    'no-case-declarations': 1,
    '@typescript-eslint/prefer-regexp-exec': 1,
    'no-constant-condition': 1,
    'no-empty': 1,
    '@typescript-eslint/restrict-plus-operands': 1,
    '@typescript-eslint/require-await': 1,
    '@typescript-eslint/no-floating-promises': 1,
    'quotes': [1, 'single', 'avoid-escape'],
    'eqeqeq': [2, 'smart'],
    'semi': [2, 'always'],
    'require-jsdoc': 0,
    'valid-jsdoc': 0,
    'no-var': 2,
    'no-console': 0,
    'prefer-const': 2,
    'comma-dangle': 0,
    'arrow-parens': 0,
    'no-trailing-spaces': 2,
    'eol-last': [2, 'always'],
    'no-ternary': 'off',
    'no-duplicate-imports': 1,
    'space-before-function-paren': [
      2,
      {
        anonymous: 'always',
        named: 'never',
        asyncArrow: 'always'
      }
    ],
    'quote-props': [2, 'as-needed']
  }
};
