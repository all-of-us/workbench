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
    // 'prettier/prettier': 'warn', // Possibly use prettier to handle some formatting

    /* Best Practices */
    'no-multi-spaces': 'error',

    /* Typescript specific */
    '@typescript-eslint/explicit-member-accessibility': 'off',
    '@typescript-eslint/member-ordering': ['error', {'default': [
      'static-field',
      'instance-field',
      'static-method',
      'instance-method'
    ]}],
    '@typescript-eslint/no-empty-interface': 'error',
    '@typescript-eslint/no-inferrable-types': ['error', {ignoreParameters: true}],
    '@typescript-eslint/no-non-null-assertion': 'error',
    // 'prefer-arrow/prefer-arrow-functions': ['warn'], // Lots of 'newable' functions in the code base
    '@typescript-eslint/type-annotation-spacing': 'error',
    '@typescript-eslint/unified-signatures': 'error',

    /* Style */
    'brace-style': ['error', '1tbs', { 'allowSingleLine': true }],
    '@typescript-eslint/consistent-type-definitions': ['error', 'interface'],
    '@typescript-eslint/naming-convention': ['error',
      {'selector': 'class', 'format': ['PascalCase']},
      {'selector': 'interface', 'format': ['PascalCase']}
    ],
    'no-trailing-spaces': 'error',
    'no-undef-init': 'error',
    '@typescript-eslint/prefer-function-type': 'error',
    'quotes': ['error', 'single', {'allowTemplateLiterals': true}],
    '@typescript-eslint/semi': 'error',
    'simple-import-sort/sort': 'error',
    'space-before-function-paren': ['error', { 'anonymous': 'never', 'named': 'never', 'asyncArrow': 'always' }],
    'spaced-comment': ['error', 'always', {'block': {'balanced': true}}],

    /* Functionality */
    'constructor-super': 'error',
    'curly': 'error',
    'guard-for-in': 'error',
    'no-restricted-imports': ['error', {paths: ['rxjs'], patterns: ['../']}],
    'no-caller': 'error',
    'no-bitwise': 'error',
    'no-console': ['error', {'allow': ['warn', 'error', 'assert']}],
    'no-new-wrappers': 'error',
    'no-debugger': 'error',
    'no-empty': 'error',
    'no-eval': 'error',
    'no-irregular-whitespace': 'error',
    '@typescript-eslint/no-misused-new': 'error',
    '@typescript-eslint/no-shadow': 'error',
    'no-throw-literal': 'error',
    'no-fallthrough': 'error', // For switch statements
    'no-use-before-define': 'off', // Needed for TS
    '@typescript-eslint/no-use-before-define': 'error',
    'no-unused-expressions': ['error', {'allowShortCircuit': true, 'allowTernary': true}],
    'no-unused-vars': 'off', // Needed for TS
    '@typescript-eslint/no-unused-vars': ['error', {'varsIgnorePattern': '^_$', 'args': 'none'}],
    'react/jsx-curly-spacing': ['error', {'when': 'never'}],
    'react/jsx-uses-vars': 'error',
    'no-var': 'error',
    'radix': 'error', // Add radix on parseInt
    'eqeqeq': ['error', 'always', {'null': 'ignore'}],
    
    /* Maintainability */
    'eol-last': 'error',
    'max-len': ['error', {code: 140, ignorePattern: '^import |^export\\{(.*?)\\}', ignoreComments: true}],
    'prefer-const': ['error', {'destructuring': 'all'}],
  }
};
