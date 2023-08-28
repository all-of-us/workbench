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
  ],
  // If we extend multiple rulesets in the future, ensure that 'prettier' is last.
  // This will allow it to disable prettier-incompatible rules from other rulesets.
  extends: ['prettier'],
  parserOptions: {
    ecmaFeatures: {
      jsx: true,
    },
    ecmaVersion: 6,
    project: ['./tsconfig.json'],
    sourceType: 'module',
  },
  settings: {
    react: {
      version: 'detect',
    },
  },
  env: {
    browser: true,
    node: true,
  },

  // Some of these rules were ported over from our common-ui/tslint.json file
  // The rules found here were ported and categorized using the tslint migration roadmap:
  // https://github.com/typescript-eslint/typescript-eslint/blob/master/packages/eslint-plugin/ROADMAP.md

  // The eslint rules can be found here: https://eslint.org/docs/rules/
  rules: {
    /* Code Quality */

    'constructor-super': 'warn',
    curly: 'warn',
    eqeqeq: ['warn', 'always', { null: 'ignore' }],
    'guard-for-in': 'warn',
    'no-bitwise': 'warn',
    'no-caller': 'warn',
    'no-debugger': 'warn',
    'no-empty': 'warn',
    'no-eval': 'warn',
    'no-fallthrough': 'warn', // For switch statements
    'no-irregular-whitespace': 'warn',
    'no-new-wrappers': 'warn',
    'no-restricted-imports': ['error', { paths: ['rxjs'], patterns: ['../'] }],
    'no-throw-literal': 'warn',
    'no-undef-init': 'warn',
    'no-var': 'warn',
    'prefer-const': ['warn', { destructuring: 'all' }],
    radix: 'warn', // Add radix on parseInt
    // 'no-console': 'warn',  // 69 instances as of 3 Jan 2022
    // 'prefer-arrow/prefer-arrow-functions': ['warn'], // Lots of 'newable' functions in the code base

    'no-use-before-define': 'off', // Needed for the below rule
    '@typescript-eslint/no-use-before-define': 'warn',

    'no-unused-vars': 'off', // Needed for the below rule
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '_' }],

    '@typescript-eslint/consistent-type-definitions': ['warn', 'interface'],
    '@typescript-eslint/dot-notation': 'warn',
    '@typescript-eslint/explicit-member-accessibility': 'off',
    '@typescript-eslint/no-empty-interface': 'warn',
    '@typescript-eslint/no-inferrable-types': [
      'warn',
      { ignoreParameters: true },
    ],
    '@typescript-eslint/no-misused-new': 'warn',
    '@typescript-eslint/no-non-null-assertion': 'warn',
    '@typescript-eslint/no-shadow': 'warn',
    '@typescript-eslint/prefer-optional-chain': 'warn',
    '@typescript-eslint/unified-signatures': 'warn',
    // '@typescript-eslint/member-ordering': ['warn', { 'classExpressions': ['method', 'field'] }],
    // '@typescript-eslint/prefer-function-type': 'warn',

    'react/jsx-uses-vars': 'warn',
    'react-hooks/rules-of-hooks': 'warn',
    // 'react-hooks/exhaustive-deps': 'warn',  // 45 instances as of 3 Jan 2022

    /* Style */

    // prettier is 'highly opinionated' and should always be the first rule in this section.
    // please confirm that other style rules are compatible with prettier before adding them.

    'prettier/prettier': 'warn',

    'max-len': [
      'warn',
      {
        code: 140,
        ignorePattern: '^import |^export\\{(.*?)\\}',
        ignoreComments: true,
      },
    ],

    'no-trailing-spaces': 'warn',
    'simple-import-sort/imports': ['warn',
      {
        // each 'regex' below matches an import `from` string
        // regexes ['are', 'arranged', 'in', 'groups'] below
        // within a [group], imports are sorted first by 'regex', then inside each 'regex'
        // so for example, 'react' and 'react-router-dom' will come before 'lodash/fp' in that group
        // groups are separated by empty lines
        'groups': [
          // Side effect imports.
          ['^\\u0000'],

          // dependencies, in importance order (YMMV, and I'm sure I missed some)
          [
            '^react', '^lodash', '^enzyme', '^highcharts', '^primereact', '^validate', '^@fortawesome', '^querystring',
            '^stacktrace', '^history', '^nouislider', '^setupTests'
          ],

          // swagger
          ['^generated'],

          // main group plus catchall - anything not matched in another group.
          ['^environments', '^'],

          ['^testing'],

          ['^./'],
        ]
      }
    ],
    'spaced-comment': 'warn',

    /* Jest */
    'jest/no-focused-tests': 'warn',
  },
};
