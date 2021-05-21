const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');

require('jest-circus');

class PuppeteerCustomEnvironment extends PuppeteerEnvironment {
  screenshotDir = 'logs/screenshot';
  htmlDir = 'logs/html';
  failedTestSuites = {};

  async setup() {
    await super.setup();
  }

  async teardown() {
    await super.teardown();
  }

  // returns a string corresponding to the local browser's date and time in this format:
  // 20201103_084435
  localDateTimeString() {
    const localTime = new Date();
    const fakeUtcTime = new Date(localTime.getTime() - localTime.getTimezoneOffset() * 60000);
    return fakeUtcTime.toISOString().replace(/[-:]/g, '').slice(0, 15);
  }

  // jest-circus: https://github.com/facebook/jest/blob/master/packages/jest-circus/README.md#overview
  // Take a screenshot right after failure
  async handleTestEvent(event, state) {
    switch (event.name) {
      case 'test_start':
        // https://stackoverflow.com/questions/51250006/jest-stop-test-suite-after-first-fail
        // When one test fails, next tests in the same describe block will be skipped.
        // If there are more describe blocks in the same test file, they will not be skipped.
        if (this.failedTestSuites[event.test.parent.name]) {
          event.test.mode = 'skip';
        }
        break;
      case 'test_fn_failure':
      case 'hook_failure':
        {
          let describeName;
          try {
            describeName = state.currentlyRunningTest.parent.name;
          } catch (err) {
            describeName = event.test.parent.name;
          }
          this.failedTestSuites[describeName] = true;
          const runningTest = state.currentlyRunningTest;
          let testName;
          if (runningTest != null) {
            testName = runningTest.name.replace(/\W/g, '-');
          } else {
            testName = event.test.name.replace(/\W/g, '-');
          }
          await fs.ensureDir(this.screenshotDir);
          await fs.ensureDir(this.htmlDir);
          const screenshotFile = `${this.screenshotDir}/${testName}.png`;
          await this.takeScreenshot(screenshotFile);
          const htmlFile = `${this.htmlDir}/${testName}.html`;
          await this.savePageToFile(htmlFile);
        }
        break;
      default:
        break;
    }
    if (super.handleTestEvent) {
      super.handleTestEvent(event, state);
    }
  }

  async takeScreenshot(filePath) {
    const [activePage] = (await this.global.browser.pages()).slice(-1);
    await activePage.screenshot({ path: filePath, fullPage: true });
    console.info(`Saved screenshot: ${filePath}`);
  }

  async savePageToFile(htmlFile) {
    const [activePage] = (await this.global.browser.pages()).slice(-1);
    const htmlContent = await activePage.content();
    return new Promise((resolve, reject) => {
      fs.writeFile(htmlFile, htmlContent, 'utf8', (error) => {
        if (error) {
          console.error('Failed to save html file. ' + error);
          reject(false);
        } else {
          console.info('Saved html file: ' + htmlFile);
          resolve(true);
        }
      });
    });
  }
}

module.exports = PuppeteerCustomEnvironment;
