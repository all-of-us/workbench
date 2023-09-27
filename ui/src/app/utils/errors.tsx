import { ErrorCode, ErrorResponse } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';

import { systemErrorStore } from './navigation';
import {
  NotificationStore,
  notificationStore,
  stackdriverErrorReporterStore,
} from './stores';

/**
 * Reports an error to Stackdriver error logging, if enabled.
 */
export function reportError(err: string) {
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
    return { statusCode: e.status, errorCode: ErrorCode.PARSE_ERROR };
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
      const errorResponse: ErrorResponse = await convertAPIError(e);
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
          if (errorResponse.errorCode === ErrorCode.USER_DISABLED) {
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

// the json() function of the Response object can only be called once; call it and store with the original.
export interface ApiErrorResponse {
  originalResponse;
  responseJson;
}
const toApiErrorResponse = async (apiError): Promise<ApiErrorResponse> => ({
  originalResponse: apiError,
  responseJson: typeof apiError.json === 'function' && (await apiError.json()),
});

export const defaultApiErrorFormatter = (
  apiError: ApiErrorResponse
): string => {
  const { originalResponse, responseJson } = apiError || {};
  const { status, statusText } = originalResponse || {};
  const { errorCode, errorUniqueId, message } = responseJson || {};

  const errorCodeStr = errorCode ? ` of type ${errorCode.toString()}` : '';
  const statusStr =
    status && statusText && `HTTP status code ${status} (${statusText})`;
  const messageStr = message ? `: ${message}` : '.';

  const detailsStr = cond(
    [
      !!status && !!statusText && !!errorUniqueId,
      () => ` with ${statusStr} and unique ID ${errorUniqueId}`,
    ],
    [!!status && !!statusText, () => ` with ${statusStr}`],
    [!!errorUniqueId, () => ` with unique ID ${errorUniqueId}`],
    () => ''
  );

  return `An API error${errorCodeStr} occurred${detailsStr}${messageStr}`;
};

export const FALLBACK_ERROR_TITLE = 'An error has occurred';

/**
 * Convert an API error response to a format suitable for an error modal.  The caller may supply a
 * matcher for expected error responses which should not pop up error modals, and a custom formatter for expected
 * failure responses; if neither of these match, the defaultApiErrorFormatter will be used.
 *
 * Example: an (async) API call getInfo() sometimes appropriately returns 404s which should not be considered errors,
 * and there's a common error case (HTTP 429, Error Code TOO_FAST) which we want to handle specially.
 *
 * const responseP = getInfo();
 * await responseP.then(resp =>
 *  errorHandlerWithFallback(
 *    resp,
 *    (er: ApiErrorResponse) => er?.originalResponse?.status === 404,
 *    (er: ApiErrorResponse) => er?.originalResponse?.status === 429 && er?.responseJson?.errorCode === ErrorCode.TOO_FAST && {
 *        title: 'Please try again',
 *        message: 'The server is currently handling too many requests',
 *      }
 *    ));
 *
 * @param apiError The error response from an API call
 * @param expectedResponseMatcher An optional handler for errors which should NOT pop up error modals
 * @param customErrorResponseFormatter An optional handler for expected responses; if missing or this handler returns
 * undefined, the default formatter will be used.
 * @returns A NotificationStore object suitable for an error modal, if appropriate; undefined otherwise
 */
export const errorHandlerWithFallback = async (
  apiError,
  expectedResponseMatcher?: (ApiErrorResponse) => boolean,
  customErrorResponseFormatter?: (ApiErrorResponse) => NotificationStore
): Promise<NotificationStore> => {
  const parsedResponse = await toApiErrorResponse(apiError);

  // if this "error" is expected and should instead be considered a success
  if (expectedResponseMatcher?.(parsedResponse)) {
    return undefined;
  }

  // the custom error response for this error, if applicable
  const customResponse = customErrorResponseFormatter?.(parsedResponse);
  return (
    customResponse || {
      title: FALLBACK_ERROR_TITLE,
      message: defaultApiErrorFormatter(parsedResponse),
    }
  );
};

interface FetchOptions {
  expectedResponseMatcher?: (ApiErrorResponse) => boolean;
  customErrorResponseFormatter?: (ApiErrorResponse) => NotificationStore;
}
/**
 * Call an (async) API function, and if unsuccessful pop up a modal with a suitable message by setting the
 * notificationStore (see NotificationModal for more details).
 *
 * The caller may supply a matcher for expected error responses which should not pop up error modals, and a custom
 * formatter for expected failure responses; if neither of these match, the defaultApiErrorFormatter will be used.
 *
 * Example: an API call getInfo() sometimes appropriately returns 404s which should not be considered errors,
 * and there's a common error case (HTTP 429, Error Code TOO_FAST) which we want to handle specially.
 *
 * fetchWithErrorModal(
 *  () => getInfo(),
 *  {
 *    expectedResponseMatcher: (er: ApiErrorResponse) => er?.originalResponse?.status === 404,
 *    customErrorResponseFormatter: (er: ApiErrorResponse) =>
 *      er?.originalResponse?.status === 429 && er?.responseJson?.errorCode === ErrorCode.TOO_FAST && {
 *        title: 'Please try again',
 *        message: 'The server is currently handling too many requests',
 *      }
 *  });
 *
 * @param apiCall The API function to call and handle
 * @param options
 *  expectedResponseMatcher An optional handler for errors which should NOT pop up error modals
 *  customErrorResponseFormatter An optional handler for expected responses; if missing or this handler returns
 * undefined, the default formatter will be used.
 * @returns A promise containing the resolution or rejection of the API function call
 */
export async function fetchWithErrorModal<T>(
  apiCall: () => Promise<T>,
  options?: FetchOptions
): Promise<T> {
  const { expectedResponseMatcher, customErrorResponseFormatter } =
    options || {};
  return apiCall().catch(async (apiError) => {
    const notification = await errorHandlerWithFallback(
      apiError,
      expectedResponseMatcher,
      customErrorResponseFormatter
    );
    if (notification) {
      notificationStore.set(notification);
    }

    // enable further chaining if desired
    throw apiError;
  });
}
