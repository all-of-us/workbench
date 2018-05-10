package org.pmiops.workbench.firecloud;

import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.net.SocketTimeoutException;

@Service
public class FirecloudRetryHandler extends RetryHandler<ApiException> {

  private static class FirecloudRetryPolicy extends RetryConfig.ResponseCodeRetryPolicy {
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
  }

  @Autowired
  public FirecloudRetryHandler(BackOffPolicy backoffPolicy) {
    super(backoffPolicy, new FirecloudRetryPolicy());
  }


  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return ExceptionUtils.convertFirecloudException(exception);
  }
}
