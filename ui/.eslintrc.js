module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: [
    'react',
    'sort-keys-fix',
    'prefer-arrow',
    '@typescript-eslint',
    'simple-import-sort',
    'prettier',
    'jest'
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
  
  // These rules were ported over from our common-ui/tslint.json file
  // The rules found here were ported and categorized using the tslint migration roadmap:
  // https://github.com/typescript-eslint/typescript-eslint/blob/master/packages/eslint-plugin/ROADMAP.md

  // The eslint rules can be found here: https://eslint.org/docs/rules/
  rules: {
    // 'prettier/prettier': 'error', // Possibly use prettier to handle some formatting

    /* Best Practices */
    // 'no-multi-spaces': 'error',

    /* Typescript specific */
    '@typescript-eslint/explicit-member-accessibility': 'off',
    // '@typescript-eslint/member-ordering': ['error', { 'classExpressions': ['method', 'field'] }],
    '@typescript-eslint/no-empty-interface': 'error',
    '@typescript-eslint/no-inferrable-types': ['error', {ignoreParameters: true}],
    '@typescript-eslint/no-non-null-assertion': 'error',
    // 'prefer-arrow/prefer-arrow-functions': ['error'], // Lots of 'newable' functions in the code base 
    '@typescript-eslint/type-annotation-spacing': 'error',
    '@typescript-eslint/unified-signatures': 'error',

    /* Style */
    '@typescript-eslint/prefer-function-type': 'error',
    // 'spaced-comment': 'error',
    '@typescript-eslint/consistent-type-definitions': ['error', 'interface'],
    'no-trailing-spaces': 'error',
    'no-undef-init': 'error',
    // 'brace-style': ['error', '1tbs'],
    // 'simple-import-sort/sort': 'error',
    'quotes': ['error', 'single'],
    // '@typescript-eslint/semi': 'error',
    // 'space-before-function-paren': ['error', { 'anonymous': 'never', 'named': 'never', 'asyncArrow': 'always' }],

    /* Functionality */
    'curly': 'error',
    'guard-for-in': 'error',
    'no-restricted-imports': ['error', {paths: ['rxjs'], patterns: ['../']}],
    'no-caller': 'error',
    'no-bitwise': 'error',
    // 'no-console': 'error', 
    'no-new-wrappers': 'error',
    'no-debugger': 'error',
    'constructor-super': 'error',
    'no-empty': 'error',
    'no-eval': 'error',
    'no-irregular-whitespace': 'error',
    '@typescript-eslint/no-misused-new': 'error',
    // 'no-shadow': 'error',
    // 'dot-notation': 'error',
    'no-throw-literal': 'error', 
    'no-fallthrough': 'error', // For switch statements
    'no-use-before-define': 'off', // Needed for TS
    // '@typescript-eslint/no-use-before-define': 'error',
    // 'no-unused-vars': 'off', // Needed for TS 
    // '@typescript-eslint/no-unused-vars': 'error', 
    'react/jsx-curly-spacing': ["warn", {'when': 'never'}],
    'react/jsx-uses-vars': 'error',
    'no-var': 'error',
    'radix': 'error', // Add radix on parseInt
    'eqeqeq': ['error', 'always', {'null': 'ignore'}],
    
    /* Maintainability */
    'eol-last': 'error',
    'max-len': ['error', {code: 140, ignorePattern: '^import |^export\\{(.*?)\\}', ignoreComments: true}], 
    // 'prefer-const': ['error', {'destructuring': 'all'}],

    /* Jest */
    'jest/no-focused-tests': 'error',
  }
};
