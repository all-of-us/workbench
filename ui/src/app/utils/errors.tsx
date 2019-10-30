import {StackdriverErrorReporter} from 'stackdriver-errors-js';

let stackdriverReporter: StackdriverErrorReporter;

/**
 * Static global setter for Stackdriver reporting. This is meant as a temporary
 * bridge between Angular and React.
 */
export function setStackdriverErrorReporter(reporter: StackdriverErrorReporter) {
  stackdriverReporter = reporter;
}

/**
 * Reports an error to Stackdriver error logging, if enabled.
 */
export function reportError(err: Error) {
  console.error(err);
  if (stackdriverReporter) {
    stackdriverReporter.report(err, (e) => {
      // Note: this does not detect non-200 responses from Stackdriver:
      // https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/32
      if (e) {
        console.error('failed to send error report: ', e);
      }
    });
  }
}
