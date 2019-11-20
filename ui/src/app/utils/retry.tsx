import {isAbortError} from 'app/utils/errors';

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
