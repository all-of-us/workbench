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
    '@typescript-eslint/no-empty-interface': 'warn',
    '@typescript-eslint/no-inferrable-types': ['warn', {ignoreParameters: true}],
    '@typescript-eslint/no-non-null-assertion': 'warn',
    // 'prefer-arrow/prefer-arrow-functions': ['warn'], // Lots of 'newable' functions in the code base
    '@typescript-eslint/type-annotation-spacing': 'warn',
    '@typescript-eslint/unified-signatures': 'warn',

    /* Style */
    'brace-style': ['warn', '1tbs', { 'allowSingleLine': true }],
    '@typescript-eslint/consistent-type-definitions': ['warn', 'interface'],
    '@typescript-eslint/naming-convention': ['error',
      {'selector': 'class', 'format': ["PascalCase"]},
      {'selector': 'interface', 'format': ["PascalCase"]}
    ],
    'no-trailing-spaces': 'warn',
    'no-undef-init': 'warn',
    '@typescript-eslint/prefer-function-type': 'error',
    'quotes': ['error', 'single', {'allowTemplateLiterals': true}],
    '@typescript-eslint/semi': 'error',
    'simple-import-sort/sort': 'error',
    'space-before-function-paren': ['error', { 'anonymous': 'never', 'named': 'never', 'asyncArrow': 'always' }],
    'spaced-comment': ['warn', 'always', {'block': {'balanced': true}}],

    /* Functionality */
    'curly': 'warn',
    'guard-for-in': 'warn',
    'no-restricted-imports': ['error', {paths: ['rxjs'], patterns: ['../']}],
    'no-caller': 'warn',
    'no-bitwise': 'warn',
    'no-console': ['warn', {'allow': ['warn', 'error', 'assert']}],
    'no-new-wrappers': 'warn',
    'no-debugger': 'warn',
    'constructor-super': 'warn',
    'no-empty': 'warn',
    'no-eval': 'warn',
    'no-irregular-whitespace': 'warn',
    '@typescript-eslint/no-misused-new': 'warn',
    '@typescript-eslint/no-shadow': 'error',
    'dot-notation': 'error',
    'no-throw-literal': 'warn', 
    'no-fallthrough': 'warn', // For switch statements
    'no-use-before-define': 'off', // Needed for TS
    '@typescript-eslint/no-use-before-define': 'error',
    'no-unused-expressions': ['error', {'allowShortCircuit': true, 'allowTernary': true}],
    'react/jsx-curly-spacing': ['warn', {'when': 'never'}],
    'react/jsx-uses-vars': 'warn',
    'no-var': 'warn',
    'radix': 'warn', // Add radix on parseInt
    'eqeqeq': ['warn', 'always', {'null': 'ignore'}],
    
    /* Maintainability */
    'eol-last': 'warn',
    'max-len': ['warn', {code: 140, ignorePattern: '^import |^export\\{(.*?)\\}', ignoreComments: true}], 
    'prefer-const': ['error', {'destructuring': 'all'}],
  }
};
