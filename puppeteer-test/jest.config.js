module.exports = {
  "verbose": true,
  "roots": [
    "<rootDir>/src"
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
    "/node_modules/"
  ],
  "testRegex": "(src/tests/.*|\\.(test|spec))\\.(ts|tsx|js)$",
  "transformIgnorePatterns": [
    "<rootDir>/node_modules/(?!tests)"
  ],
  "moduleFileExtensions": [
    "ts", "tsx", "js", 'jsx', 'json', 'node'
  ]
};
