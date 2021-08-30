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
    "^.+\\.tsx?$": "ts-jest"
  },
  "testMatch": [
    "**/*.spec.tsx"
  ],
  "globals": {
    "ts-jest": {
      "tsconfig": "<rootDir>/src/tsconfig.jest.json"
    }
  }
}

module.exports = config;
