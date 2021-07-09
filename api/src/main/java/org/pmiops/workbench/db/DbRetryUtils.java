package org.pmiops.workbench.db;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;

/** Utilities to execute database operations with retry support. */
public final class DbRetryUtils {
  private static final Logger logger = Logger.getLogger(DbRetryUtils.class.getName());

  private DbRetryUtils() {}

  /**
   * Executes a database operation and retries if retryable
   *
   * @param execute database operation to execute
   * @param retrySleep fixed retry sleep interval
   * @param maxNumAttempts maximum times this operation will be run
   * @param <T> database operation class
   * @return database operation class
   * @throws InterruptedException on thread interruption
   * @throws DataAccessException throws the last DB operation error if maxNumAttempts is exceeded.
   */
  public static <T> T executeAndRetry(
      DatabaseOperation<T> execute, Duration retrySleep, int maxNumAttempts)
      throws InterruptedException {
    Preconditions.checkArgument(maxNumAttempts > 0, "maxNumAttempts must be at least 1");
    int numAttempts = 1;
    while (numAttempts <= maxNumAttempts) {
      try {
        return execute.execute();
      } catch (DataAccessException e) {
        if (!shouldRetryQuery(e) || (numAttempts == maxNumAttempts)) {
          throw e;
        }
        logger.log(
            Level.WARNING,
            String.format(
                "Caught exception, retrying DB operation. Attempts so far: %d", numAttempts),
            e);
      }
      ++numAttempts;
      TimeUnit.MILLISECONDS.sleep(retrySleep.toMillis());
    }
    throw new IllegalStateException(
        "Exceeded maximum number of retries without throwing an exception. This should never happen.");
  }

  /**
   * Tests an exception to see if it is retryable.
   *
   * @param dataAccessException execption to test
   * @return {@code true} if that is retryable {@link DataAccessException}.
   */
  public static boolean shouldRetryQuery(DataAccessException dataAccessException) {
    return ExceptionUtils.hasCause(dataAccessException, RecoverableDataAccessException.class)
        || ExceptionUtils.hasCause(dataAccessException, TransientDataAccessException.class);
  }

  /** How to execute this database operation. */
  @FunctionalInterface
  public interface DatabaseOperation<T> {
    T execute();
  }
}
