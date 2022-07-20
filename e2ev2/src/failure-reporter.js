const fs = require('fs')

class FailureReporter {
  onTestResult(test, testResult) {
    // Stole this "isFailed" check from another reporter.
    if (!testResult.skipped && testResult.failureMessage) {
      fs.writeFileSync(logFileName, test.path.slice(process.env.PWD.length+1)+'\n', {flag: 'a'})
    }
  }
}

module.exports = FailureReporter
