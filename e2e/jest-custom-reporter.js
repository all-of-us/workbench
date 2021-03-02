// @ts-nocheck
const fs = require('fs-extra');
const path = require('path');

module.exports = class JestCustomReporter {

  constructor(globalConfig, options) {
    this._globalConfig = globalConfig;
    this._options = options;
    this._consoleLogs = [];
    if (globalConfig.verbose === true) {
      throw Error("Invalid configuration. Verbose must be false or Console messages won't be available.")
    }
    this.testResults = [];
    this.logDir = this._options.outputdir || 'logs/jest';
  }

  // onTestResult() {
    // tslint:disable-next-line:no-console
    // console.log('onTestResult start');
    /*
    if (testResult.console) {
      for (const logObj of testResult.console) {
        this._logMessages.push(logObj.message)
      }
    }
    // tslint:disable-next-line:no-console
    console.log(`onTestResult: ${JSON.stringify(this._logMessages)}`);

    const testPath = path.parse(test.path);
    const testResultsDir = path.join(
      testPath.dir,
      this._options.outputdir || 'logs',
    );
    const testFileName = testPath.base.replace(/\./g, '-');
    // tslint:disable-next-line:no-console
    console.log(testResultsDir);
    // tslint:disable-next-line:no-console
    console.log(`testFileName: ${testFileName}`);
    */
 //  }

  onTestResult(testRunConfig, testResult, runResults) {
    const testName = path.parse(testResult.testFilePath).name;
    console.log(`fileName: ${testName}`);

    testResult.testResults.forEach((result) => {
      const failure = result.failureMessage;
      console.log(`failure: ${failure}`);

      // testResult contains console logs if Jest config verbose: false
      if (testResult.console && testResult.console.length > 0) {
        testResult.console.forEach(log => {
          console.log(`log: ${log.message}`);
        });
      }

      if (result.status === "failed") {
        try {
          const stack = result.failureDetails[0].stack.split("\n");

          const frame1 = stack.findIndex((row) => row.startsWith("    at"));
          const location = stack[frame1].match(/[^:]*:([^:]*:[^:]*)/);

          console.log(
            `-- failed;${testResult.testFilePath};${
              location[1]
            };${result.failureDetails[0].message.replace(/\n/g, " ")}`
          );
        } catch (e) {
          console.log("ERROR", e);
        }
      }

      this.saveLog(`${testName}.json`, JSON.stringify(testResult, null, 2), this.logDir);

    });



    this.testResults.push(testResult);
    console.log('');
    console.log(this._consoleLogs);
    console.log('');
  }

  onRunComplete(test, runResults) {
    const fileName = this._options.filename || 'jest-test-results-summary.json';
    runResults.testResults.forEach(result => {
      const testFilePath = result.testFilePath.split('e2e/')[1];
      const failure = [];
      result.testResults.forEach(test => {
        if (test.status === 'failed') {
          failure.push(`yarn test ${testFilePath}`);
          // tslint:disable-next-line:no-console
          console.log(failure);
        }
      })
    });

    this.saveLog(fileName, JSON.stringify(this.testResults, null, 2), this.logDir);
    console.log("📦 reporter:", fileName);
    return runResults;
  }


  trimTestResult(testResult) {
    console.log(`trimTestResults: ${testResult}`);
    testResult.snapshot = undefined;
    testResult.perfStats = undefined;
    console.log(`trimTestResults: ${testResult}`);
    return testResult;
  }

  saveLog(logFileName, contents, dir) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir);
    }
    fs.writeFileSync(`${dir}/${logFileName}`, contents, 'utf-8');
    console.log(`Saved ${logFileName} to ${dir}`);
  }

}

