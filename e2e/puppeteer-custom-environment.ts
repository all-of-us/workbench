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
  async handleTestEvent(event, state) {
    if (event.name === 'test_fn_failure') {

        console.log(`event.name = ${event.name}`);

        const testParentName = state.currentlyRunningTest.parent.name.replace(/\W/g, '-');
        const testName = state.currentlyRunningTest.name.replace(/\W/g, '-');

        const screenshotDir = `logs/screenshot/${testParentName}`;
        const htmlDir = `logs/html/${testParentName}`;
        await fs.ensureDir(screenshotDir);
        await fs.ensureDir(htmlDir);

        console.error(`Failed test:  "${testParentName}/${testName}"`);

        const timestamp = new Date().getTime();

        const screenshotFile = `${screenshotDir}/${testName}_${timestamp}.png`;
        await this.takeScreenshot(screenshotFile);

        const htmlFile = `${htmlDir}/${testName}_${timestamp}.html`;
        await this.savePageToFile(htmlFile);
    }
  }

  async takeScreenshot(filePath) {
    await this.global.page.screenshot({path: filePath, fullPage: true});
    console.info(`Saved screenshot ${filePath}`);
  }

  async savePageToFile(htmlFile) {
    const htmlContent = await this.global.page.content();
    return new Promise((resolve, reject) => {
      fs.writeFile(htmlFile, htmlContent, 'utf8', error => {
        if (error) {
          console.error(`Failed to save html file. ` + error);
          reject(false);
        } else {
          console.info('Saved html file ' + htmlFile);
          resolve(true);
        }
      })
    });
  }

}

module.exports = PuppeteerCustomEnvironment;
