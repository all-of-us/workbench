// const NodeEnvironment = require('jest-environment-node');
const PuppeteerEnvironment = require('jest-environment-puppeteer');
const fs = require('fs-extra');
const path = require('path');

require('jest-circus');

// TODO Replace PuppeteerEnvironment with NodeEnvironment: https://precisionmedicineinitiative.atlassian.net/browse/RW-6967
class PuppeteerCustomEnvironment extends PuppeteerEnvironment {
  screenshotDir = 'logs/screenshot';
  htmlDir = 'logs/html';
  failedTestSuites = {};

  constructor(config, context) {
    super(config, context);
    this.testPath = context.testPath;
    this.global.__SPEC_NAME__ = path.parse(this.testPath).name;
  }

  async setup() {
    await super.setup();
  }

  async teardown() {
    await super.teardown();
  }

  getVmContext() {
    return super.getVmContext();
  }

  getNames(parent) {
    if (!parent || parent.name === 'ROOT_DESCRIBE_BLOCK') {
      return [];
    }
    const parentName = this.getNames(parent.parent);
    return [...parentName, parent.name];
  }

  // jest-circus: https://github.com/facebook/jest/blob/master/packages/jest-circus/README.md#overview
  async handleTestEvent(event, state) {
    const { name } = event;
    if (['test_start', 'test_fn_start'].includes(name)) {
      this.global.__TEST_NAMES__ = this.getNames(event.test);
      // https://stackoverflow.com/questions/51250006/jest-stop-test-suite-after-first-fail
      // When one test fails, next tests in the same describe block will be skipped.
      // If there are more describe blocks in the same test file, they will not be skipped.
      if (this.failedTestSuites[this.global.__TEST_NAMES__[0]]) {
        event.test.mode = 'skip';
      }
    }
    if (['hook_failure', 'test_fn_failure'].includes(name)) {
      const describeBlockName = this.global.__TEST_NAMES__[0];
      this.failedTestSuites[describeBlockName] = true;
      // TODO Remove: https://precisionmedicineinitiative.atlassian.net/browse/RW-6967
      await fs.ensureDir(this.screenshotDir);
      await fs.ensureDir(this.htmlDir);
      const screenshotFile = `${this.global.__SPEC_NAME__}.png`;
      const htmlFile = `${this.global.__SPEC_NAME__}.html`;
      const [activePage] = (await this.global.browser.pages()).slice(-1);
      await takeScreenshot(activePage, screenshotFile);
      await savePageToFile(activePage, htmlFile);
    }
    if (super.handleTestEvent) {
      super.handleTestEvent(event, state);
    }
  }
}

// TODO Remove: https://precisionmedicineinitiative.atlassian.net/browse/RW-6967
const takeScreenshot = async (page, fileName) => {
  const dir = 'logs/screenshot';
  await fs.ensureDir(dir);
  await page.screenshot({ type: 'png', path: `${dir}/${fileName}`, fullPage: true });
  console.info(`Saved screenshot file: ${fileName}`);
};

// TODO Remove: https://precisionmedicineinitiative.atlassian.net/browse/RW-6967
const savePageToFile = async (page, fileName) => {
  const dir = 'logs/html';
  await fs.ensureDir(dir);
  const htmlContent = await page.content();
  return new Promise((resolve, reject) => {
    fs.writeFile(`${dir}/${fileName}`, htmlContent, 'utf8', (error) => {
      if (error) {
        console.error('Failed to save html file. ' + error);
        reject(false);
      } else {
        console.info(`Saved html file: ${fileName}`);
        resolve(true);
      }
    });
  });
};

module.exports = PuppeteerCustomEnvironment;
