package org.pmiops.workbench.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class GoogleRetryHandler extends RetryHandler<IOException> {

  private static final Logger logger = Logger.getLogger(GoogleRetryHandler.class.getName());

  private static class GoogleRetryPolicy extends RetryConfig.ResponseCodeRetryPolicy {

    public GoogleRetryPolicy() {
      super("Google API");
    }

    @Override
    protected int getResponseCode(Throwable lastException) {
      if (lastException instanceof GoogleJsonResponseException) {
        return ((GoogleJsonResponseException) lastException).getStatusCode();
      }
      if (lastException instanceof SocketTimeoutException) {
        return HttpServletResponse.SC_GATEWAY_TIMEOUT;
      }
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    protected boolean canRetry(int code) {
      // Google services are known to throw 500 errors sometimes when it would be appropriate
      // to retry. So we will retry in these cases, too.
      return super.canRetry(code) || code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    protected void logNoRetry(Throwable t, int responseCode) {
      if (t instanceof GoogleJsonResponseException) {
        logger.log(
            getLogLevel(responseCode),
            String.format(
                "Exception calling Google API with response: %s",
                ((GoogleJsonResponseException) t).getDetails()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public GoogleRetryHandler(BackOffPolicy backOffPolicy) {
    super(backOffPolicy, new GoogleRetryPolicy());
  }

  @Override
  protected WorkbenchException convertException(IOException exception) {
    return ExceptionUtils.convertGoogleIOException(exception);
  }
}
