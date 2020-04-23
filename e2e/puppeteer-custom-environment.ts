const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');
require('jest-circus');


// jest-circus retryTimes
const retryTimes = process.env.RETRY_ATTEMPTS || 1;

class PuppeteerCustomEnvironment extends PuppeteerEnvironment {
  async setup() {
    await super.setup();
  }

  async teardown() {
    // time for screenshots
    await this.global.page.waitFor(1000);
    await super.teardown();
  }

  // Take a screenshot right after failure
  async handleTestEvent(event, state) {
    switch (event.name) {
      case 'test_fn_start':
        console.log('test start: \t', event.test.name);
        break;
      case 'test_fn_success':
        console.log('test passed: \t', event.test.name);
        break;
      case 'test_fn_failure':
        console.log('test failed: \t', event.test.name);
        if (state.currentlyRunningTest.invocations > retryTimes) {
          const testName = state.currentlyRunningTest.name.replace(/\s/g, ''); // remove whitespaces
          const screenshotDir = 'logs/screenshot';
          await fs.ensureDir(screenshotDir);
          // move create-filename to helper.ts
          const timestamp = new Date().getTime();
          const fileName = `${testName}_${timestamp}.png`
          const screenshotFile = `${screenshotDir}/${fileName}`;
          await this.global.page.screenshot({path: screenshotFile, fullPage: true});
          console.error(`Test "${event.test.name}" failed. Saved screenshot ${screenshotFile}.`);
        }
        break;
      default:
        break;
    }
  }

}

module.exports = PuppeteerCustomEnvironment;
