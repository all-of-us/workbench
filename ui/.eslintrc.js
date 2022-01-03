module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: [
    '@typescript-eslint',
    'jest',
    'prefer-arrow',
    'prettier',
    'react',
    'react-hooks',
    'simple-import-sort',
    'sort-keys-fix',
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

  // Some of these rules were ported over from our common-ui/tslint.json file
  // The rules found here were ported and categorized using the tslint migration roadmap:
  // https://github.com/typescript-eslint/typescript-eslint/blob/master/packages/eslint-plugin/ROADMAP.md

  // The eslint rules can be found here: https://eslint.org/docs/rules/
  rules: {
    // 'prettier/prettier': 'warn', // Possibly use prettier to handle some formatting

    /* Best Practices */
    // 'no-multi-spaces': 'warn',

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
    '@typescript-eslint/prefer-function-type': 'warn',
    // 'spaced-comment': 'warn',
    '@typescript-eslint/consistent-type-definitions': ['warn', 'interface'],
    'no-trailing-spaces': 'warn',
    'no-undef-init': 'warn',
    // 'brace-style': ['warn', '1tbs'],
    // 'simple-import-sort/sort': 'warn',
    'quotes': ['warn', 'single'],
    // '@typescript-eslint/semi': 'warn',
    // 'space-before-function-paren': ['warn', { 'anonymous': 'never', 'named': 'never', 'asyncArrow': 'always' }],

    /* Functionality */
    'curly': 'warn',
    'guard-for-in': 'warn',
    'no-restricted-imports': ['error', {paths: ['rxjs'], patterns: ['../']}],
    'no-caller': 'warn',
    'no-bitwise': 'warn',
    // 'no-console': 'warn',  // 69 instances as of 3 Jan 2022
    'no-new-wrappers': 'warn',
    'no-debugger': 'warn',
    'constructor-super': 'warn',
    'no-empty': 'warn',
    'no-eval': 'warn',
    'no-irregular-whitespace': 'warn',
    '@typescript-eslint/no-misused-new': 'warn',
    // 'no-shadow': 'warn', // 54 instances as of 3 Jan 2022
    // 'dot-notation': 'warn',  // 39 instances as of 3 Jan 2022
    'no-throw-literal': 'warn',
    'no-fallthrough': 'warn', // For switch statements
    // 'no-use-before-define': 'off', // Needed for the below rule
    // '@typescript-eslint/no-use-before-define': 'warn', // 55 instances as of 3 Jan 2022
    // 'no-unused-vars': 'off', // Needed for the below rule
    // '@typescript-eslint/no-unused-vars': 'warn', // 271 instances as of 3 Jan 2022
    'no-var': 'warn',
    'radix': 'warn', // Add radix on parseInt
    'eqeqeq': ['warn', 'always', {'null': 'ignore'}],

    /* React */
    'react/jsx-curly-spacing': ["warn", {'when': 'never'}],
    'react/jsx-uses-vars': 'warn',
    'react-hooks/rules-of-hooks': 'warn',
    //'react-hooks/exhaustive-deps': 'warn',

    /* Maintainability */
    'eol-last': 'warn',
    'max-len': ['warn', {code: 140, ignorePattern: '^import |^export\\{(.*?)\\}', ignoreComments: true}],
    // 'prefer-const': ['warn', {'destructuring': 'all'}],

    /* Jest */
    'jest/no-focused-tests': 'warn',
  }
};
