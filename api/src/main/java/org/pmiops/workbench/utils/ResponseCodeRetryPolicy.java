package org.pmiops.workbench.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

public abstract class ResponseCodeRetryPolicy extends SimpleRetryPolicy {

  private static final Logger logger = Logger.getLogger(ResponseCodeRetryPolicy.class.getName());

  private final String serviceName;

  public ResponseCodeRetryPolicy(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public boolean canRetry(RetryContext context) {
    // canRetry is (counter-intuitively) invoked before the first attempt;
    // in that scenario, getLastThrowable() returns null and we should proceed.
    if (context.getLastThrowable() == null) {
      return true;
    }
    Throwable lastException = context.getLastThrowable();
    int responseCode = getResponseCode(lastException);
    if (canRetry(responseCode)) {
      if (context.getRetryCount() < getMaxAttempts()) {
        logRetry(context.getRetryCount(), lastException);
        return true;
      } else {
        logGivingUp(context.getRetryCount(), lastException);
        return false;
      }
    } else {
      logNoRetry(lastException, responseCode);
      return false;
    }
  }

  protected boolean canRetry(int code) {
    return ExceptionUtils.isServiceUnavailable(code);
  }

  protected void logRetry(int retryCount, Throwable t) {
    logger.log(
        Level.WARNING,
        String.format("%s unavailable, retrying after %d attempts", serviceName, retryCount),
        t);
  }

  protected void logGivingUp(int retryCount, Throwable t) {
    logger.log(
        Level.WARNING,
        String.format("%s unavailable, giving up after %d attempts", serviceName, retryCount),
        t);
  }

  protected Level getLogLevel(int responseCode) {
    switch (responseCode) {
      case HttpServletResponse.SC_NOT_FOUND:
        return Level.INFO;
      case HttpServletResponse.SC_UNAUTHORIZED:
      case HttpServletResponse.SC_FORBIDDEN:
      case HttpServletResponse.SC_CONFLICT:
        return Level.WARNING;
      default:
        return Level.SEVERE;
    }
  }

  protected void logNoRetry(Throwable t, int responseCode) {
    logger.log(getLogLevel(responseCode), String.format("Exception calling %s", serviceName), t);
  }

  protected abstract int getResponseCode(Throwable lastException);
}
