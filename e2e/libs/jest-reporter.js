const fs = require('fs-extra');
const path = require('path');
const { fileLogger } = require('./logger');
const winston = require("winston");

module.exports = class JestReporter {

  constructor(globalConfig, options) {
    if (globalConfig.verbose === true) {
      throw Error("Verbose must be false or Console messages won't be available.")
    }
    this._globalConfig = globalConfig;
    this._options = options;
    this.logDir = this._options.outputdir || 'logs/jest';
    this.summaryFile = this._options.filename || 'test-results-summary.json';
  }

  onTestStart(test) {
    const time = new Date().toLocaleTimeString();
    console.info(`Running ${path.parse(test.path).name} at ${time}`);
  }

  // @ts-ignore
  onTestResult(testRunConfig, testResult, runResults) {
    // Get test name.
    const today = new Date();
    const testName = path.parse(testResult.testFilePath).name;
    const testLog = `${this.logDir}/${testName}-${today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate()}.log`;

    const transports = new winston.transports.File({
        filename: testLog,
        options: { flags: 'w' },
        handleExceptions: true,
      });
    fileLogger.clear().add(transports);

    // Get all console logs.
    if (testResult.console && testResult.console.length > 0) {
      testResult.console.forEach((log) => {
        fileLogger.info(log.message);
      });
    }

    // Get failure messages.
    testResult.testResults.forEach((test) => {
      fileLogger.info('----------------------------------------');
      fileLogger.log('info', 'test name: %s', test.title);
      fileLogger.log('info', 'status: %s', test.status);
      // Get failure message.
      if (test.failureMessages) {
        const failure = test.failureMessages;
        fileLogger.log('info', 'failure: %s', failure);
      }
    });
    console.log(`Save test log: ${testLog}`);
  }

  // @ts-ignore
  onRunComplete(test, runResults) {
    runResults.testResults.forEach(suite => {
      const testFilePath = suite.testFilePath.split('e2e/')[1];
      const failedTests = [];
      suite.testResults.forEach(result => {
        if (result.status === 'failed') {
          failedTests.push(`yarn test ${testFilePath}`);
        }
      });
      if (failedTests.length > 0) {
        console.info(`****  To rerun failed tests:\n      ${failedTests}`);
      }
    });

    // Save test results to a file.
    if (!fs.existsSync(this.logDir)) fs.mkdirSync(this.logDir);
    fs.writeFileSync(`${this.logDir}/${this.summaryFile}`, JSON.stringify(runResults, null, 2));
    console.info(`Save tests results summary file: ${this.logDir}/${this.summaryFile}`);
    return runResults;
  }

}
