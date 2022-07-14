const fs = require('fs')

const logFileName = 'failed-tests.txt'
const fileExists = p => {
  try { fs.accessSync(p, fs.constants.R_OK); return true } catch (e) { return false }
}

class FailureReporter {
  onRunStart() {
    if (fileExists(logFileName)) {
      fs.rmSync(logFileName)
    }
  }

  onTestResult(test, testResult) {
    if (!testResult.skipped) {
      const hasFailures = testResult.failureMessage

      if (hasFailures) {
        fs.writeFileSync(logFileName, test.path.slice(process.env.PWD.length+1)+'\n', {flag: 'a'})
      }
    }
  }
}

module.exports = FailureReporter
