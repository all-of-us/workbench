const fs = require('fs-extra');
const path = require('path');
const winston = require('winston');

module.exports = class JestReporter {
  constructor(globalConfig, options) {
    if (globalConfig.verbose === true) {
      throw Error("Verbose must be false or Console messages won't be available.");
    }
    this._options = options;
    this.logDir = this._options.outputdir || 'logs/jest';
    this.summaryFile = this._options.filename || 'test-results-summary.json';
  }

  timeNow() {
    return new Date().toLocaleString('en-US', {
      timeZone: 'America/New_York',
      hour12: false
    });
  }

  onTestStart(test) {
    console.info(`Running ${path.parse(test.path).name} at ${this.timeNow()}`);
  }

  onTestResult(_testRunConfig, testResult, _runResults) {
    const testName = path.parse(testResult.testFilePath).name;
    let testLogName = `${this.logDir}/${testName}.log`;
    testResult.testResults.forEach((result) => {
      const status = result.status;
      if (status === 'failed') {
        testLogName = `${this.logDir}/${testName}-FAILED.log`;
      }
    });

    const fileLogger = winston.createLogger({
      level: process.env.LOG_LEVEL || 'info',
      format: winston.format.combine(
        winston.format.splat(),
        winston.format.printf((info) => {
          return `${info.message}`;
        })
      ),
      transports: [
        new winston.transports.File({
          filename: testLogName,
          options: { flags: 'w' },
          handleExceptions: true
        })
      ],
      exitOnError: false
    });

    // Read jest console messages and save to a log file.
    // Get all console logs.
    if (testResult.console && testResult.console.length > 0) {
      testResult.console.forEach((log) => {
        fileLogger.info(log.message);
      });
    }

    // Get failure messages.
    testResult.testResults.forEach((result) => {
      fileLogger.info('----------------------------------------');
      fileLogger.log('info', 'test name: %s', result.title);
      fileLogger.log('info', 'status: %s', result.status);
      // Get failure message.
      if (result.failureMessages) {
        const failure = result.failureMessages;
        fileLogger.log('info', 'failure: %s', failure);
      }
    });
    console.log(`Save test log: ${testLogName}`);
  }

  onRunComplete(_test, runResults) {
    runResults.testResults.forEach((suite) => {
      const testFilePath = suite.testFilePath.split('e2e/')[1];
      const failedTests = [];
      suite.testResults.forEach((result) => {
        if (result.status === 'failed') {
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
    fs.writeFileSync(`${this.logDir}/${this.summaryFile}`, JSON.stringify(runResults, null, 2));
    console.info(`Save tests results summary file: ${this.logDir}/${this.summaryFile}`);
    return runResults;
  }
};
