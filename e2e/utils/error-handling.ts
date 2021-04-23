import { logger } from 'libs/logger';

export const withErrorLogging = (opts: { fn: (...args) => any; message?: string }): any => {
  const { fn, message } = opts;
  return async (...args) => {
    try {
      await fn(...args);
    } catch (err) {
      message && logger.error(message);
      logger.error(err);
      logger.error(err.stack);
      throw new Error(err);
    }
  };
};

export const withErrorIgnoring = (fn: (...args) => any): any => {
  return async (...args) => {
    try {
      await fn(...args);
    } catch (err) {
      // Ignored
    }
  };
};
