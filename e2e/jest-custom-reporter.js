const fs = require('fs-extra');
const path = require('path');
const util = require('util');

module.exports = class JestCustomReporter {

  constructor(globalConfig, options) {
    this._globalConfig = globalConfig;
    this._options = options;
    this._consoleLogs = [];
    if (globalConfig.verbose === true) {
      throw Error("Invalid configuration. Verbose must be false or Console messages won't be available.")
    }
    this.logDir = this._options.outputdir || 'logs/jest';
    this.fileName = this._options.filename || 'test-results-summary.json';
  }

  onTestStart(test) {
    this.log(`Starting ${path.parse(test.path).name} at ${new Date().toLocaleTimeString('en-US',{timeZone:'America/New_York'})}`);
  }

  // @ts-ignore
  onTestResult(testRunConfig, testResult, runResults) {
    const consoleLogs = [];

    // Get test name.
    const testName = path.parse(testResult.testFilePath).name;
    consoleLogs.push(testName);
    consoleLogs.push('');

    // Get all console logs.
    if (testResult.console && testResult.console.length > 0) {
      testResult.console.forEach((log) => {
        consoleLogs.push(util.format(log.message));
      });
    }

    // Iterate testResults array.
    testResult.testResults.forEach((suite) => {
      // Get failure message.
      if (suite.testResults && suite.testResults.status === "failed") {
        const failure = suite.testResults.failureMessages;
        consoleLogs.push(`failure: ${failure}`);
      }
    });
    const data = consoleLogs.reduce((a, b) => a + util.inspect(b, { maxArrayLength: null, compact: false }) + `\n`, "");
    this.saveLog(`${testName}.log`, data, this.logDir);
  }

  // @ts-ignore
  onRunComplete(test, runResults) {
    this.log('CUSTOM REPORTER');
    this.failTestsSummary(runResults);

    runResults.testResults.forEach(suite => {
      suite.console.forEach(log => {
        JSON.stringify(log.messsage, null, 2);
      })
    });

    // Write all test results to a file.
    this.saveLog(this.fileName, JSON.stringify(runResults, null, 2), this.logDir);

    return runResults;
  }

  failTestsSummary(runResults) {
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
  }

  saveLog(logFileName, contents, dir) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir);
    }
    fs.writeFileSync(`${dir}/${logFileName}`, contents);
    this.log(`Saved file: ${dir}/${logFileName}`);
  }

  log(message) {
    console.info(`****  ${message}  ****`);
  }

}
