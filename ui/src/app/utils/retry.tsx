import { isAbortError } from 'app/utils/errors';

// Retry a fetch `maxRetries` number of times with a `timeoutMillis` wait between retries
// Respects fetch aborts
export async function fetchAbortableRetry<T>(
  fetchFn: () => Promise<T>,
  timeoutMillis: number,
  maxRetries: number
): Promise<T> {
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
      await new Promise((resolve) => setTimeout(resolve, timeoutMillis));
    }
  }
}

async function apiCallWithGatewayTimeoutRetriesAndRetryCount<T>(
  apiCall: () => Promise<T>,
  maxRetries = 3,
  retryCount = 1,
  initialWaitTime = 1000
): Promise<T> {
  try {
    return await apiCall();
  } catch (ex) {
    if (ex.status !== 504 || retryCount > maxRetries) {
      throw ex;
    }
    await new Promise((resolve) =>
      setTimeout(resolve, initialWaitTime * Math.pow(2, retryCount))
    );
    return await apiCallWithGatewayTimeoutRetriesAndRetryCount(
      apiCall,
      maxRetries,
      retryCount + 1,
      initialWaitTime
    );
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
  apiCall: () => Promise<T>,
  maxRetries = 3,
  initialWaitTime = 1000
): Promise<T> {
  return apiCallWithGatewayTimeoutRetriesAndRetryCount(
    apiCall,
    maxRetries,
    1,
    initialWaitTime
  );
}
