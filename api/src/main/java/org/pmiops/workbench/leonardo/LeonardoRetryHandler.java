package org.pmiops.workbench.leonardo;

import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class LeonardoRetryHandler extends RetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(LeonardoRetryHandler.class.getName());

  private static class LeonardoRetryPolicy extends ResponseCodeRetryPolicy {

    public LeonardoRetryPolicy() {
      super("Leonardo API");
    }

    @Override
    protected int getResponseCode(Throwable lastException) {
      if (lastException instanceof ApiException) {
        return ((ApiException) lastException).getCode();
      }
      if (lastException instanceof SocketTimeoutException) {
        return HttpServletResponse.SC_GATEWAY_TIMEOUT;
      }
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    protected void logNoRetry(Throwable t, int responseCode) {
      if (t instanceof ApiException) {
        if (responseCode == 404) {
          logger.log(
              getLogLevel(responseCode),
              String.format(
                  "Exception calling Leonardo API with response: %s. This is likely because a runtime "
                      + "has not yet been created and is being polled for. Suppressing stack trace.",
                  responseCode));
        } else {
          logger.log(
              getLogLevel(responseCode),
              String.format(
                  "Exception calling Leonardo API with response: %s",
                  ((ApiException) t).getResponseBody()),
              t);
        }
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public LeonardoRetryHandler(BackOffPolicy backoffPolicy) {
    super(backoffPolicy, new LeonardoRetryPolicy());
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return ExceptionUtils.convertLeonardoException(exception);
  }
}
