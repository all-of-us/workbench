const fs = require('fs-extra');
const path = require('path');
const winston = require('winston');

module.exports = class JestReporter {
  testName;
  testLogName;
  logger;

  constructor(globalConfig, options) {
    if (globalConfig.verbose === true) {
      throw Error("Verbose must be false or Console messages won't be available.");
    }
    this.logDir = options.outputdir || 'logs/jest';
    this.summaryFile = options.filename || 'test-results-summary.json';
  }

  // Called at the beginning of every test file
  onTestStart(test) {
    console.info(`Running ${path.parse(test.path).name} at ${this.timeNow()}`);
    this.testName = path.parse(test.path).name;
    this.testLogName = `${this.logDir}/${this.testName}.log`;
  }

  // Called with the result of every test file
  onTestResult(_testRunConfig, testResult, _runResults) {
    const hasFailure = testResult.testResults.some((result) => {
      return result.status === 'failed';
    });
    if (hasFailure) {
      // Save logs of failed test
      this.testLogName = `${this.logDir}/${this.testName}-FAILED.log`;
      this.logger = this.createLogger(this.testLogName);

      // Read jest console messages and save to a log file.
      // Get all console logs.
      if (testResult.console) {
        testResult.console.forEach((log) => {
          this.logger.info(log.message);
        });
      }

      // Get failure messages.
      this.logger.info('\n\nTests Summary');
      testResult.testResults.forEach((result) => {
        this.logger.info('----------------------------------------------');
        this.logger.log('info', 'test name: %s', result.title);
        this.logger.log('info', 'status: %s', result.status);
        // Get failure message.
        if (result.failureMessages) {
          const failure = result.failureMessages;
          this.logger.log('info', 'failure: %s', failure);
        }
        this.logger.info('');
      });
      console.log(`Saved log of failed test: ${this.testLogName}`);
    }
  }

  // Called after all tests have completed
  onRunComplete(_test, runResults) {
    // Save test results to a file.
    if (!fs.existsSync(this.logDir)) {
      fs.mkdirSync(this.logDir);
    }
    // Save Summary log
    fs.writeFileSync(`${this.logDir}/${this.summaryFile}`, JSON.stringify(runResults, null, 2));
    console.info(`Save tests results summary file: ${this.logDir}/${this.summaryFile}`);
    return runResults;
  }

  timeNow() {
    return new Date().toLocaleString('en-US', {
      timeZone: 'America/New_York',
      hour12: false
    });
  }

  createLogger(fileName) {
    const logger = winston.createLogger({
      level: process.env.LOG_LEVEL || 'info',
      format: winston.format.combine(
        winston.format.splat(),
        winston.format.printf((info) => {
          return `${info.message}`;
        })
      ),
      transports: [
        new winston.transports.File({
          filename: fileName,
          options: { flags: 'w' },
          handleExceptions: true
        })
      ],
      exitOnError: false
    });
    return logger;
  }
};
