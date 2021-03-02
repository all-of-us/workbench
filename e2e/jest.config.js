const { defaults } = require('jest-config');
const { TEST_MODE } = process.env;

module.exports = {
  "verbose": false,
  "bail": 1,  // Stop running tests after `n` failures
  "preset": "jest-puppeteer",
  "testTimeout": 1200000,
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
    ["jest-stare", {
        "resultDir": "logs",
        "resultJson": "test-results.json",
        "reportTitle": "AoU integration tests",
        "report": true
      }
    ],
    ["jest-junit", {
        outputDirectory: "./logs/junit",
        outputName: "test-results.xml",
        classNameTemplate: "{filepath}",
        suiteNameTemplate: "{filepath}",
        suiteName: "Puppeteer end-to-end tests"
      }
    ],
    ["<rootDir>/jest-custom-reporter.js", {
        "outputdir": "logs/jest",
        "filename": "jest-test-results.json"
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
