const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');
require('jest-circus');

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
  async handleTestEvent(event) {
    switch (event.name) {
      // case 'test_fn_failure':
      // case 'hook_failure':
      case 'test_done':
        if (event.test.errors.length > 0) {
          console.error(`handleTestEvent case: ${event.name}`);
          console.error(`Failed test:  "${event.test.name}"`);
          const testName = event.test.name.replace(/\W/g, '-');
          const screenshotDir = 'logs/screenshot';
          await fs.ensureDir(screenshotDir);
          // move create-filename to helper.ts
          const timestamp = new Date().getTime();
          const screenshotFileName = `${screenshotDir}/${testName}_${timestamp}.png`;
          await this.global.page.screenshot({path: screenshotFileName, fullPage: true});
          console.error(`Saved screenshot ${screenshotFileName}`);
          const htmlFileName = `${testName}_${timestamp}.html`;
          await this.savePageToFile(htmlFileName);
        }
        break;
      default:
        break;
    }
  }

  async savePageToFile(fileName) {
    const logDir = 'logs/html';
    await fs.ensureDir(logDir);
    const htmlFile = `${logDir}/${fileName}`;
    const htmlContent = await this.global.page.content();
    return new Promise((resolve, reject) => {
      fs.writeFile(htmlFile, htmlContent, 'utf8', error => {
        if (error) {
          console.error(`Failed to save html file. ` + error);
          reject(false);
        } else {
          console.log('Saved html file ' + htmlFile);
          resolve(true);
        }
      })
    });
  }

}

module.exports = PuppeteerCustomEnvironment;
