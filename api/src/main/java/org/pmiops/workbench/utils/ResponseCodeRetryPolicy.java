package org.pmiops.workbench.utils;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    // Retry om 500, 502, 503, 504 error.
    return ExceptionUtils.isServiceUnavailable(code)
        || ExceptionUtils.isInternalServerError(code)
        || ExceptionUtils.isGatewayTimeoutServerError(code);
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
    logger.log(
        getLogLevel(responseCode),
        String.format(
            "Exception calling %s with response: HTTP %d, %s",
            serviceName, responseCode, describeResponseBody(t)),
        t);
  }

  /**
   * The response body of the failed call, or null if this service cannot supply one. Each service
   * client is generated separately, so their exception types share no interface exposing it.
   */
  @Nullable
  protected String getResponseBody(Throwable lastException) {
    return null;
  }

  /**
   * Some services answer with no body at all: VWB User Manager rejects deleting an already inactive
   * notification with a bare 404, for example. Logging the body alone left nothing to go on, so say
   * the body was empty and fall back to the exception message.
   */
  private String describeResponseBody(Throwable t) {
    String responseBody = getResponseBody(t);
    if (responseBody != null && !responseBody.isBlank()) {
      return responseBody;
    }
    return String.format("<empty body> (%s)", t.getMessage());
  }

  protected abstract int getResponseCode(Throwable lastException);
}
