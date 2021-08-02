const NodeEnvironment = require('jest-environment-node');
const puppeteer = require("puppeteer");
const fs = require('fs-extra');
const path = require('path');
const util = require('./utils/save-file-utils');
require('jest-circus');

export class PuppeteerCustomEnvironment extends NodeEnvironment {
  screenshotDir = 'logs/screenshot';
  htmlDir = 'logs/html';
  failedTestSuites = {};

  constructor(config, context) {
    super(config, context);
    this.testPath = context.testPath;
    this.global.__SPEC_NAME__ = path.parse(this.testPath).name;
  }

  async setup(): Promise<void> {
    await super.setup();
    this.global.browser = await puppeteer.launch({
      headless: false,
      slowMo: 100
    })
    this.global.page = await this.global.browser.newPage()
  }

  async teardown(): Promise<void> {
    await this.global.browser.close();
    await super.teardown();
  }

  getVmContext() {
    return super.getVmContext();
  }

  getNames(parent): string[] {
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
      await fs.ensureDir(this.screenshotDir);
      await fs.ensureDir(this.htmlDir);
      const screenshotFile = `${this.global.__SPEC_NAME__}.png`;
      const htmlFile = `${this.global.__SPEC_NAME__}.html`;
      const [activePage] = (await this.global.browser.pages()).slice(-1);
      await util.takeScreenshot(activePage, screenshotFile);
      await util.savePageToFile(activePage, htmlFile);
    }
    if (super.handleTestEvent) {
      super.handleTestEvent(event, state);
    }
  }
}

module.exports = PuppeteerCustomEnvironment;
