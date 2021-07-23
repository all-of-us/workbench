const NodeEnvironment = require('jest-environment-node');
const fs = require('fs-extra');
const path = require('path');
require('jest-circus');

class PuppeteerEnvironment extends NodeEnvironment {
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
    }
    if (super.handleTestEvent) {
      super.handleTestEvent(event, state);
    }
  }
}

module.exports = PuppeteerEnvironment;
