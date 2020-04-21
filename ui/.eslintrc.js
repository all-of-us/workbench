module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  extends: ["react-app", "prettier"],
  plugins: [
    'react',
    'sort-keys-fix',
    'prefer-arrow',
    '@typescript-eslint',
    'simple-import-sort',
    'prettier'
  ],
  parserOptions: {
    ecmaVersion: 6,
    sourceType: "module",
    ecmaFeatures: {
      jsx: true // Allows for the parsing of JSX
    }
  },
  settings: {
    react: {
      version: "detect" // Tells eslint-plugin-react to automatically detect the version of React to use
    }
  },
  extends: [],
  rules: {
    // "prettier/prettier": "warn",
    // Typescript specific
    "@typescript-eslint/explicit-member-accessibility": "off",
    "@typescript-eslint/member-ordering": ["warn", { "classExpressions": ["method", "field"] }],
    "@typescript-eslint/no-empty-interface": "warn",
    "@typescript-eslint/no-inferrable-types": ["warn", {ignoreParameters: true}],
    "@typescript-eslint/no-non-null-assertion": "warn",
    // "prefer-arrow/prefer-arrow-functions": ["warn"],
    "@typescript-eslint/type-annotation-spacing": "warn",
    "@typescript-eslint/unified-signatures": "warn",


    // Style
    "arrow-body-style": "warn",
    "@typescript-eslint/prefer-function-type": "warn",
    "@typescript-eslint/class-name-casing": "warn",
    "capitalized-comments": "warn",
    "spaced-comment": "warn",
    "@typescript-eslint/consistent-type-definitions": ["warn", "interface"],
    "no-trailing-spaces": "warn",
    "no-undef-init": "warn",
    "brace-style": ["warn", "stroustrup", { "allowSingleLine": true }],
    "simple-import-sort/sort": "warn",
    "quotes": ["warn", "single"],
    "@typescript-eslint/semi": "warn",
    "@typescript-eslint/naming-convention": "warn",
    "space-before-function-paren": ["warn", "never"],
    // "import-spacing": "warn", -- use prettier

    // Functionality
    "curly": "warn",
    "guard-for-in": "warn",
    "no-restricted-imports": ["error", {paths: ["rxjs"] }],
    "no-unused-labels": "warn",
    "no-caller": "warn",
    "no-bitwise": "warn",
    "no-console": "warn",
    "no-new-wrappers": "warn",
    "no-debugger": "warn",
    "constructor-super": "warn",
    "no-empty": "warn",
    "no-eval": "warn",
    "no-irregular-whitespace": "warn",
    "@typescript-eslint/no-misused-new": "warn",
    "no-shadow": "warn",
    "dot-notation": "warn",
    // "@typescript-eslint/no-throw-literal": "warn", -- CORRECT
    "no-fallthrough": "warn",
    "no-unused-expressions": "warn",
    "@typescript-eslint/no-use-before-define": "warn",
    "no-var": "warn",
    "radix": "warn",
    "eqeqeq": ["warn", "always", {"null": "ignore"}],
    
    //Maintainability
    "eol-last": "warn",
    "max-len": ["warn", {code: 140, ignorePattern: "^import |^export\\{(.*?)\\}"}], 
    "sort-keys-fix/sort-keys-fix": "warn",
    "prefer-const": ["warn", {"destructuring": "all"}],


    // "whitespace": [ // CONFIGURE PRETTIER
    //   "warn",
    //   "check-branch",
    //   "check-decl",
    //   "check-operator",
    //   "check-separator",
    //   "check-type"
    // ],

    // ALL ANGULAR RULES BELOW THIS
    // "directive-selector": [
    //   "warn",
    //   "attribute",
    //   ["app", "crit"],
    //   "camelCase"
    // ],
    // "component-selector": [
    //   "warn",
    //   "element",
    //   ["app", "crit"],
    //   "kebab-case"
    // ],

    // "use-input-property-decorator": "warn",
    // "use-output-property-decorator": "warn",
    // "use-host-property-decorator": "warn",
    // "no-input-rename": false,
    // "no-output-rename": false,
    // "use-life-cycle-interface": "warn",
    // "use-pipe-transform-interface": "warn",
    // "component-class-suffix": [
    //   "warn",
    //   "Component",
    //   "Page",
    //   "Layout"
    // ],
    // "directive-class-suffix": "warn",
    // "space-before-function-paren": [true, "never"]
  }
};