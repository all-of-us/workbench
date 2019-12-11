import {convertAPIError, isAbortError} from 'app/utils/errors';
import {globalErrorStore} from 'app/utils/navigation';

import {ErrorCode} from 'generated/fetch';

// Retry a fetch `maxRetries` number of times with a `timeoutMillis` wait between retries
// Respects fetch aborts
export async function fetchAbortableRetry<T>(fetchFn: () => Promise<T>, timeoutMillis: number, maxRetries: number): Promise<T> {
  let retries = 0;
  while (true) {
    try {
      return await fetchFn();
    } catch (e) {
      retries++;
      if (isAbortError(e) || retries >= maxRetries) {
        throw e;
      }
      // effectively a sleep for timeoutMillis
      await new Promise(resolve => setTimeout(resolve, timeoutMillis));
    }
  }
}



export async function fetchWithGlobalErrorHandler<T>(fetchFn: () => Promise<T>, maxRetries: number = 3): Promise<T> {
  let retries = 0;
  while (true) {
    try {
      const returnVal = await fetchFn();
      console.log(returnVal);
      return returnVal;
    } catch (e) {
      retries++;
      const errorResponse = await convertAPIError(e);
      if (retries === maxRetries) {
        globalErrorStore.next(errorResponse);
        throw e;
      }
      switch (errorResponse.statusCode) {
        case 503:
          // Only retry on 503s
          break;
        case 500:
          globalErrorStore.next(errorResponse);
          throw e;
        case 403:
          if (errorResponse.errorCode === ErrorCode.USERDISABLED) {
            globalErrorStore.next(errorResponse);
          }
          throw e;
        case 0:
          globalErrorStore.next(errorResponse);
          throw e;
        default:
          throw e;
      }
    }
  }
}

/*
 * A method to run an api call with a specified number of retries and exponential backoff.
 * This method will only error
 * Parameters:
 *    apiCall: Lambda that will run an API call, in the form of () => apiClient.apiCall(args)
 *    maxRetries: The amount of retries the system will take before erroring
 *    defaultWaitTime: How long the base exponential backoff is, in milliseconds
 *      For example, if 1000 is passed in, it will wait 1s for the first retry,
 *      2s for the second, etc.
 */
export async function apiCallWithGatewayTimeoutRetries<T>(
  apiCall: () => Promise<T>, maxRetries = 3, initialWaitTime = 1000): Promise<T> {
  return apiCallWithGatewayTimeoutRetriesAndRetryCount(apiCall, maxRetries, 1, initialWaitTime);
}

async function apiCallWithGatewayTimeoutRetriesAndRetryCount<T>(
  apiCall: () => Promise<T>, maxRetries = 3, retryCount = 1, initialWaitTime = 1000): Promise<T> {
  try {
    return await apiCall();
  } catch (ex) {
    if (ex.status !== 504 || retryCount > maxRetries) {
      throw ex;
    }
    await new Promise(resolve => setTimeout(resolve, initialWaitTime * Math.pow(2, retryCount)));
    return await apiCallWithGatewayTimeoutRetriesAndRetryCount(
      apiCall, maxRetries, retryCount + 1, initialWaitTime);
  }
}
