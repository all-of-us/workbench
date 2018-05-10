package org.pmiops.workbench.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;

@Service
public class GoogleRetryHandler extends RetryHandler<IOException> {

  private static class GoogleRetryPolicy extends RetryConfig.ResponseCodeRetryPolicy {
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
