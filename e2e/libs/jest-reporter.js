const fs = require('fs-extra');
const path = require('path');
const winston = require('winston');

module.exports = class JestReporter {
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
  }

  // Called with the result of every test file
  onTestResult(_testRunConfig, testResult, _runResults) {
    const hasFailure = testResult.testResults.some((result) => {
      return result.status === 'failed';
    });
    if (hasFailure) {
      // Save logs of failed test
      const testName = path.parse(testResult.testFilePath).name;
      const testLogName = `${this.logDir}/${testName}-FAILED.log`;
      const logger = this.createLogger(testLogName);

      // Read jest console messages and save to a log file.
      // Get all console logs.
      if (testResult.console) {
        testResult.console.forEach((log) => {
          logger.info(log.message);
        });
      }

      // Get failure messages.
      logger.info('\n\nTests Summary');
      testResult.testResults.forEach((result) => {
        logger.info('----------------------------------------------');
        logger.log('info', 'test name: %s', result.title);
        logger.log('info', 'status: %s', result.status);
        // Get failure message.
        if (result.failureMessages) {
          const failure = result.failureMessages;
          logger.log('info', 'failure: %s', failure);
        }
        logger.info('');
      });
      console.log(`Saved log of failed test: ${testLogName}`);
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
    const loggerInstance = winston.createLogger({
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
    return loggerInstance;
  }
};
