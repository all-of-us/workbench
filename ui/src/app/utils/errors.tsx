import {ErrorCode} from 'generated/fetch';
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
export function reportError(err: (Error|string)) {
  console.error('Reporting error to Stackdriver: ', err);
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

/** Returns true if the given error is an AbortError, as used in fetch() aborts. */
export function isAbortError(e: Error) {
  return e instanceof DOMException && e.name === 'AbortError';
}

// convert error response from API JSON to ErrorResponse object, otherwise, report parse error
export async function convertAPIError(e) {
  try {
    const {errorClassName = null,
      errorCode = null,
      errorUniqueId = null,
      message = null,
      statusCode = null} = await e.json();
    return { errorClassName, errorCode, errorUniqueId, message, statusCode };
  }  catch {
    return { statusCode: e.status, errorCode: ErrorCode.PARSEERROR };
  }
}
