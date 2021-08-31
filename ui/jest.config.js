const config = {
  "preset": "ts-jest",
    "clearMocks": true,
    "moduleFileExtensions": [
    "ts",
    "tsx",
    "js",
    "json",
    "node"
  ],
  "moduleDirectories": [
    "node_modules",
    "src"
  ],
  "transform": {
    "^.+\\.tsx?$": "ts-jest",
    "\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$": "<rootDir>/src/testing/fileTransformer.js",
  },
  "testMatch": [
    "**/*.spec.tsx"
  ],
  "globals": {
    "ts-jest": {
      "tsconfig": "<rootDir>/src/tsconfig.jest.json"
    }
  },
  "setupFiles": ["<rootDir>/src/testing/navigation-mock.ts"],
  "setupFilesAfterEnv": ["<rootDir>/src/setupTests.js"]
}

module.exports = config;
