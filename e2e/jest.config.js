const { defaults } = require('jest-config');
const { TEST_MODE } = process.env;

module.exports = {
  "verbose": false,
  "bail": 1,  // Stop running tests after `n` failures
  "preset": "jest-puppeteer",
  "testTimeout": 600000,
  "testRunner": "jest-circus/runner",
  "testEnvironment": "<rootDir>/puppeteer-custom-environment.ts",
  "setupFilesAfterEnv": [
    "<rootDir>/jest-circus.setup.ts",
    "<rootDir>/jest.test-setup.ts"
  ],
  setupFiles: [
    "dotenv/config"
  ],
  "reporters": [
    "default",
    "jest-junit",
    [
      "jest-stare", {
        "resultDir": "logs",
        "resultJson": "test-results.json",
        "reportTitle": "AoU integration tests",
        "report": false
      }
    ]
  ],
  "transform": {
    "^.+\\.(ts|tsx)$": "ts-jest"
  },
  "globals": {
    "ts-jest": {
      "tsconfig": "tsconfig.jest.json"
    }
  },
  "testPathIgnorePatterns": [
    "/node_modules/",
    "/tsc-out/"
  ],
  testMatch: TEST_MODE === "nightly-integration" ? [ "<rootDir>/tests/nightly/**/*.spec.ts", ] : [ "<rootDir>/tests(?!\/nightly)/**/*.spec.ts", ],
  "transformIgnorePatterns": [
    "<rootDir>/node_modules/(?!tests)"
  ],
  "moduleFileExtensions": [ ...defaults.moduleFileExtensions ],
  "modulePaths": [
    "<rootDir>"
  ]
};
