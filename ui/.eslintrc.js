module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: [
    'react',
    'sort-keys-fix',
    'prefer-arrow',
    '@typescript-eslint',
    'simple-import-sort',
    'prettier'
  ],
  extends: [],
  parserOptions: {
    ecmaVersion: 6,
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true 
    }
  },
  settings: {
    react: {
      version: 'detect' 
    }
  },
  rules: {
    // 'prettier/prettier': 'warn',

    /* Typescript specific */
    '@typescript-eslint/explicit-member-accessibility': 'off',
    // '@typescript-eslint/member-ordering': ['warn', { 'classExpressions': ['method', 'field'] }],
    '@typescript-eslint/no-empty-interface': 'warn',
    '@typescript-eslint/no-inferrable-types': ['warn', {ignoreParameters: true}],
    '@typescript-eslint/no-non-null-assertion': 'warn',
    // 'prefer-arrow/prefer-arrow-functions': ['warn'], // Lots of 'newable' functions in the code base 
    '@typescript-eslint/type-annotation-spacing': 'warn',
    '@typescript-eslint/unified-signatures': 'warn',

    /* Style */
    // 'arrow-body-style': 'warn',
    '@typescript-eslint/prefer-function-type': 'warn',
    '@typescript-eslint/class-name-casing': 'warn',
    // 'spaced-comment': 'warn',
    '@typescript-eslint/consistent-type-definitions': ['warn', 'interface'],
    'no-trailing-spaces': 'warn',
    'no-undef-init': 'warn',
    // 'brace-style': ['warn', '1tbs', { 'allowSingleLine': true }],
    // 'simple-import-sort/sort': 'warn',
    // 'quotes': ['warn', 'single'],
    // '@typescript-eslint/semi': 'warn',
    '@typescript-eslint/naming-convention': 'warn',
    // 'space-before-function-paren': ['warn', 'never'],

    /* Functionality */
    'curly': 'warn',
    'guard-for-in': 'warn',
    'no-restricted-imports': ['error', {paths: ['rxjs'] }],
    'no-unused-labels': 'warn',
    'no-caller': 'warn',
    'no-bitwise': 'warn',
    // 'no-console': 'warn',
    'no-new-wrappers': 'warn',
    'no-debugger': 'warn',
    'constructor-super': 'warn',
    'no-empty': 'warn',
    'no-eval': 'warn',
    'no-irregular-whitespace': 'warn',
    '@typescript-eslint/no-misused-new': 'warn',
    // 'no-shadow': 'warn',
    // 'dot-notation': 'warn',
    'no-throw-literal': 'warn',
    'no-fallthrough': 'warn',
    // 'no-unused-expressions': 'warn',
    // 'no-use-before-define': 'off',
    // '@typescript-eslint/no-use-before-define': 'warn',
    'no-unused-vars': 'off',
    // '@typescript-eslint/no-unused-vars': 'warn', 
    'react/jsx-uses-vars': 'warn', 
    // 'no-var': 'warn',
    'radix': 'warn',
    'eqeqeq': ['warn', 'always', {'null': 'ignore'}],
    
    /* Maintainability */
    // 'eol-last': 'warn',
    // 'max-len': ['warn', {code: 140, ignorePattern: '^import |^export\\{(.*?)\\}'}], 
    // 'sort-keys-fix/sort-keys-fix': 'warn',
    // 'prefer-const': ['warn', {'destructuring': 'all'}],
  }
};
