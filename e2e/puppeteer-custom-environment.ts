const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');
require('jest-circus');

class PuppeteerCustomEnvironment extends PuppeteerEnvironment {

  async setup() {
    await super.setup();
  }

  async teardown() {
    // time for screenshots
    await this.global.page.waitForTimeout(1000);
    await super.teardown();
  }

  // returns a string corresponding to the local browser's date and time in this format:
  // 20201103_084435
  localDateTimeString() {
    const localTime = new Date();
    const fakeUtcTime = new Date(localTime.getTime() - (localTime.getTimezoneOffset() * 60000));
    return fakeUtcTime
        .toISOString()
        .replace(/[-:]/g,'')
        .replace('T', '_')
        .slice(0, 15);
  }

  // Take a screenshot right after failure
  /*
  async handleTestEvent(event, state) {
    switch (event.name) {
      case 'test_fn_failure':
      case 'hook_failure':
        const runningTest = state.currentlyRunningTest;
        let testName;
        if (runningTest != null) {
          testName = state.currentlyRunningTest.name.replace(/\W/g, '-');
        } else {
          testName = event.test.name.replace(/\W/g, '-');
        }

        const screenshotDir = `logs/screenshot`;
        const htmlDir = `logs/html`;
        await fs.ensureDir(screenshotDir);
        await fs.ensureDir(htmlDir);

        const timestamp = this.localDateTimeString();

        const screenshotFile = `${screenshotDir}/${testName}_${timestamp}.png`;
        await this.takeScreenshot(screenshotFile);

        const htmlFile = `${htmlDir}/${testName}_${timestamp}.html`;
        await this.savePageToFile(htmlFile);
        break;
      default:
        break;
    }

  }
  */
  async takeScreenshot(filePath) {
    await this.global.page.screenshot({path: filePath, fullPage: true});
    console.info(`Saved screenshot: ${filePath}`);
  }

  async savePageToFile(htmlFile) {
    const htmlContent = await this.global.page.content();
    return new Promise((resolve, reject) => {
      fs.writeFile(htmlFile, htmlContent, 'utf8', error => {
        if (error) {
          console.error(`Failed to save html file. ` + error);
          reject(false);
        } else {
          console.info('Saved html file: ' + htmlFile);
          resolve(true);
        }
      })
    });
  }

}

module.exports = PuppeteerCustomEnvironment;
