const fs = require('fs-extra');
require('jest-circus');
const NodeEnvironment = require('jest-environment-node');

class PuppeteerCustomEnvironment extends NodeEnvironment {
  screenshotDir = 'logs/screenshot';
  htmlDir = 'logs/html';
  failedTestSuites = {};

  constructor(config, context) {
    super(config, context);
    this.testPath = context.testPath;
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
    if (!parent) {
      return [];
    }

    if (parent.name === 'ROOT_DESCRIBE_BLOCK') {
      return [];
    }

    const parentName = this.getNames(parent.parent);
    return [...parentName, parent.name];
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
  async handleTestEvent(event, _state) {
    const { name } = event;
    if (['test_start', 'test_fn_start'].includes(name)) {
      this.global.testNames = this.getNames(event.test);
    }
  }
}

module.exports = PuppeteerCustomEnvironment;
