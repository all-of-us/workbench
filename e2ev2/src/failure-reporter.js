const fs = require('fs')

const logFileName = 'failed-tests.txt'
const fileExists = p => {
  try { fs.accessSync(p, fs.constants.R_OK); return true } catch (e) { return false }
}

class FailureReporter {
  constructor(globalConfig, options = {}) {
    this._globalConfig = globalConfig;
  }

  onRunStart() {
    if (fileExists(logFileName)) {
      console.log('\n', fs.readFileSync(logFileName, 'utf8'))
      fs.rmSync(logFileName)
    }
  }

  onTestResult(test, testResult) {
    if (!testResult.skipped) {
      const hasFailures = testResult.failureMessage

      if (hasFailures) {
        fs.writeFileSync(logFileName, test.path.slice(process.env.PWD.length+1)+'\n', {flag: 'a'})
      }
      if (testResult.failureMessage)
        console.log('\n', testResult.failureMessage);
      if (testResult.console) {
        testResult.console
          .filter(entry => ['error', 'warn'].includes(entry.type) && entry.message)
          .map(entry => entry.message)
          .forEach(console.log);
      }
    }
  }
}

module.exports = FailureReporter
