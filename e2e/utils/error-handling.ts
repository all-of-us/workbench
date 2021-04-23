import { logger } from 'libs/logger';

export const withErrorLogging = async (opts: { fn: (...args) => any; message?: string }): Promise<any> => {
  const { fn, message } = opts;
  return async (...args) => {
    try {
      return await fn(...args);
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
      return await fn(...args);
    } catch (err) {
      // Ignored
    }
  };
};
