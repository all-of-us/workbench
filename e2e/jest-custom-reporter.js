const fs = require('fs-extra');
const path = require('path');
const { simpleLogger } = require('./libs/logger');
const winston = require("winston");

module.exports = class JestCustomReporter {

  constructor(globalConfig, options) {
    console.log(`onRunStart arguments: ${JSON.stringify(arguments)}`);
    /*if (globalConfig.verbose === true) {
      throw Error("Invalid configuration. Verbose must be false or Console messages won't be available.")
    }*/
    this._globalConfig = globalConfig;
    this._options = options;
    this.logDir = this._options.outputdir || 'logs/jest';
    this.fileName = this._options.filename || 'test-results-summary.json';
  }

  onTestStart(test) {
    const time = new Date().toLocaleTimeString();
    console.info(`Starting ${path.parse(test.path).name} at ${time}`);
  }

  onTestResult(testRunConfig, testResult, runResults) {
    console.log(`onTestResult arguments: ${JSON.stringify(arguments)}`);

    // Get test name.
    const testName = path.parse(testResult.testFilePath).name;
    const today = new Date();
    const transports = new winston.transports.File({
        filename: `${this.logDir}/${testName}-${today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate()}.log`,
        options: { flags: 'w' },
        handleExceptions: true,
      });
    simpleLogger.clear().add(transports);

    // Get all console logs.
    if (testResult.console && testResult.console.length > 0) {
      testResult.console.forEach((log) => {
        // simpleLogger.info(util.format(log.message));
        simpleLogger.info(log.message);
      });
    }

    // Iterate testResults array.
    testResult.testResults.forEach((suite) => {

      console.log(`suite: ${suite}`);
      // Get failure message.
      if (suite.testResults && suite.testResults.status === "failed") {
        console.log(`suite.testResults.status: ${suite.testResults.status}`);
        const failure = suite.failureMessages;
        simpleLogger.info(`failure: ${failure}`);
      }
    });

  }

  // @ts-ignore
  onRunComplete(test, runResults) {
    //console.log(`onRunComplete arguments: ${JSON.stringify(arguments)}`);
    runResults.testResults.forEach(suite => {
      const testFilePath = suite.testFilePath.split('e2e/')[1];
      const failedTests = [];
      suite.testResults.forEach(test => {
        if (test.status === 'failed') {
          failedTests.push(`yarn test ${testFilePath}`);
        }
      });
      if (failedTests.length > 0) {
        console.info(`****  To rerun failed tests:\n      ${failedTests}`);
      }
    });

    // Save test results to a file.
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir);
    }
    fs.writeFileSync(`${this.logDir}/${this.fileName}`, JSON.stringify(runResults, null, 2));
    console.info(`Saved file: ${this.logDir}/${this.fileName}`);
    return runResults;
  }

}
