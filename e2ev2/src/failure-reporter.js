const fs = require('fs')

class FailureReporter {
  onTestResult(test, testResult) {
    // Stole this "isFailed" check from another reporter.
    if (!testResult.skipped && testResult.failureMessage) {
      fs.writeFileSync(process.env.FAILED_TESTS_LOG,
        test.path.slice(process.env.PWD.length+1)+'\n', {flag: 'a'})
    }
  }
}

module.exports = FailureReporter
