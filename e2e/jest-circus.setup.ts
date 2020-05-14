require('expect-puppeteer');

// Runs failed tests n-times until they pass or until the max number of retries is exhausted. This only works with jest-circus.
// This file loaded in jest.config.js ==> setupFilesAfterEnv
jest.retryTimes(1);
