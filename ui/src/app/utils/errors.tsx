import { ErrorCode, ErrorResponse } from 'generated/fetch';

import { systemErrorStore } from './navigation';
import { stackdriverErrorReporterStore } from './stores';

/**
 * Reports an error to Stackdriver error logging, if enabled.
 */
export function reportError(err: Error | string) {
  console.error('Reporting error to Stackdriver: ', err);
  const reporterStore = stackdriverErrorReporterStore.get();
  if (reporterStore?.reporter) {
    reporterStore.reporter.report(err, (e) => {
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
export async function convertAPIError(e): Promise<ErrorResponse> {
  try {
    const {
      errorClassName = null,
      errorCode = null,
      errorUniqueId = null,
      message = null,
      statusCode = null,
    } = await e.json();
    return { errorClassName, errorCode, errorUniqueId, message, statusCode };
  } catch {
    return { statusCode: e.status, errorCode: ErrorCode.PARSEERROR };
  }
}

/*
 * A method to run an api call with our system-error handling. It also adds retries on 503
 * errors. This will convert errors to our JavaScript object, and push them to our system
 * error handler.
 * Parameters:
 *    fetchFn: Lambda that will run an API call, in the form of () => apiClient.apiCall(args)
 *    maxRetries?: The number of times it will retry before failing. Defaults to 3.
 */
export async function fetchWithSystemErrorHandler<T>(
  fetchFn: () => Promise<T>,
  maxRetries: number = 3
): Promise<T> {
  let retries = 0;
  while (true) {
    try {
      return await fetchFn();
    } catch (e) {
      retries++;
      const errorResponse = await convertAPIError(e);
      if (retries === maxRetries) {
        systemErrorStore.next(errorResponse);
        throw e;
      }
      switch (errorResponse.statusCode) {
        case 503:
          // Only retry on 503s
          break;
        case 500:
          systemErrorStore.next(errorResponse);
          throw e;
        case 403:
          if (errorResponse.errorCode === ErrorCode.USERDISABLED) {
            systemErrorStore.next(errorResponse);
          }
          throw e;
        case 0:
          systemErrorStore.next(errorResponse);
          throw e;
        default:
          throw e;
      }
    }
  }
}
