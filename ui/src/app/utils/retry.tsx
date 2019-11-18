import {isAbortError} from 'app/utils/errors';

export async function retry<T>(f: () => Promise<T>, timeoutMillis: number, maxRetries: number): Promise<T> {
  let retries = 0;
  let timeoutReference;
  while (true) {
    try {
      return await f();
    } catch (e) {
      retries++;
      if (isAbortError(e) || retries >= maxRetries) {
        clearTimeout(timeoutReference);
        throw e;
      }
      clearTimeout(timeoutReference);
      timeoutReference = setTimeout(f, timeoutMillis);
    }
  }
}
