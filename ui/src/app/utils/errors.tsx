import { ErrorCode, ErrorResponse } from 'generated/fetch';

import { cond } from './index';
import { systemErrorStore } from './navigation';
import {
  NotificationStore,
  notificationStore,
  stackdriverErrorReporterStore,
} from './stores';

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

// TODO handle errorClassName and parameters?
export const defaultErrorResponseFormatter = (
  errorResponse: ErrorResponse
): string => {
  const { message, statusCode, errorCode, errorUniqueId } = errorResponse;

  const errorCodeStr =
    errorCode && errorCode !== ErrorCode.PARSEERROR
      ? ` of type ${errorCode.toString()}`
      : '';
  const messageStr = message ? `: ${message}` : '.';

  const detailsStr = cond(
    [
      !!statusCode && !!errorUniqueId,
      () =>
        ` with HTTP status code ${statusCode} and unique ID ${errorUniqueId}`,
    ],
    [!!statusCode, () => ` with HTTP status code ${statusCode}`],
    [!!errorUniqueId, () => ` with unique ID ${errorUniqueId}`],
    () => ''
  );

  return `An API error${errorCodeStr} occurred${detailsStr}${messageStr}`;
};

export const FALLBACK_ERROR_TITLE = 'An error has occurred';

/**
 * Convert an API error response (of any type) to a format suitable for an error modal.  The caller may supply a
 * matcher for expected error responses which should be considered successes, and a custom formatter for expected
 * failure responses; if neither of these match, the defaultErrorResponseFormatter will be used.
 *
 * Example: an (async) API call getInfo() sometimes appropriately returns 404s which should not be considered errors,
 * and there's a common error case (HTTP 429, Error Code TOO_FAST) which we want to handle specially.
 *
 * const responseP = getInfo();
 * await responseP.then(resp =>
 *  errorHandlerWithFallback(
 *    resp,
 *    (er) => er.statusCode === 404,
 *    (er) =>
 *      er.statusCode === 429 && er.errorCode === ErrorCode.TOO_FAST && {
 *        title: 'Please try again',
 *        message: 'The server is currently handling too many requests',
 *      }
 *    ));
 *
 * @param anyApiErrorResponse The error response from an API call, of any type
 * @param expectedResponseMatcher An optional handler for errors which should be considered successes
 * @param customErrorResponseFormatter An optional handler for expected responses; if missing or this handler returns
 * undefined, the default formatter will be used.
 * @returns A NotificationStore object suitable for an error modal, if appropriate; undefined otherwise
 */
export const errorHandlerWithFallback = (
  anyApiErrorResponse: any,
  expectedResponseMatcher?: (ErrorResponse) => boolean,
  customErrorResponseFormatter?: (ErrorResponse) => NotificationStore
): Promise<NotificationStore> =>
  convertAPIError(anyApiErrorResponse).then((errorResponse) => {
    // if this "error" is expected and should instead be considered a success
    if (expectedResponseMatcher?.(errorResponse)) {
      return undefined;
    }

    // the custom error response for this error, if applicable
    return (
      customErrorResponseFormatter?.(errorResponse) || {
        title: FALLBACK_ERROR_TITLE,
        message: defaultErrorResponseFormatter(errorResponse),
      }
    );
  });

/**
 * Call an (async) API function and execute another function if successful.  If not, pop up a modal with a suitable
 * messagemby setting the notificationStore (see NotificationModal for more details).
 *
 * The caller may supply a matcher for expected error responses which should be considered successes, and a custom
 * formatter for expected failure responses; if neither of these match, the defaultErrorResponseFormatter will be used.
 *
 * Example: an API call getInfo() sometimes appropriately returns 404s which should not be considered errors,
 * and there's a common error case (HTTP 429, Error Code TOO_FAST) which we want to handle specially.
 *
 * fetchWithErrorModal(
 *  () => getInfo(),
 *  (er) => er.statusCode === 404,
 *  (er) =>
 *    er.statusCode === 429 && er.errorCode === ErrorCode.TOO_FAST && {
 *      title: 'Please try again',
 *      message: 'The server is currently handling too many requests',
 *    }
 *  );
 *
 * @param apiCall The API function to call and handle
 * @param onSuccess The (optional) action to take if the API call is successful
 * @param expectedResponseMatcher An optional handler for errors which should be considered successes
 * @param customErrorResponseFormatter An optional handler for expected responses; if missing or this handler returns
 * undefined, the default formatter will be used.
 * @returns The result of the API function call, if successful; undefined otherwise
 */
export async function fetchWithErrorModal<T, U>(
  apiCall: () => Promise<T>,
  onSuccess?: (T) => U,
  expectedResponseMatcher?: (ErrorResponse) => boolean,
  customErrorResponseFormatter?: (ErrorResponse) => NotificationStore
): Promise<U> {
  return apiCall()
    .then(onSuccess)
    .catch(async (apiError) => {
      const notification = await errorHandlerWithFallback(
        apiError,
        expectedResponseMatcher,
        customErrorResponseFormatter
      );
      if (notification) {
        notificationStore.set(notification);
      }
      // changes the return signature from Promise<U | void> to Promise<U>
      return undefined;
    });
}
