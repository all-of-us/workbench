module.exports = {
  "verbose": true,
  "preset": "jest-puppeteer",
  "testTimeout": 300000,
  "reporters": [
    "default",
    "jest-junit"
  ],
  "transform": {
    "\\.(ts|tsx)$": "ts-jest"
  },
  "globals": {
    "ts-jest": {
      "tsConfig": "tsconfig.jest.json"
    }
  },
  "testPathIgnorePatterns": [
    "/node_modules/",
    "/tsc-out/"
  ],
  "testMatch": [
    "<rootDir>/**/tests/**/*.spec.ts"
  ],
  "transformIgnorePatterns": [
    "<rootDir>/node_modules/(?!tests)"
  ],
  "moduleFileExtensions": [
    "ts",
    "spec.ts",
    "tsx",
    "js",
    "jsx",
    "json",
    "node"
  ],
  "modulePaths": [
    "<rootDir>"
  ]
};
