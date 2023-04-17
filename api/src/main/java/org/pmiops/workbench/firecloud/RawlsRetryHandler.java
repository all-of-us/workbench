package org.pmiops.workbench.firecloud;

import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.TerraServiceRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;
import org.pmiops.workbench.rawls.ApiException;

@Service
public class RawlsRetryHandler extends TerraServiceRetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(RawlsRetryHandler.class.getName());

  private static class FirecloudRetryPolicy extends ResponseCodeRetryPolicy {

    public FirecloudRetryPolicy() {
      super("Firecloud API");
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
        logger.log(
            getLogLevel(responseCode),
            String.format(
                "Exception calling Rawls API with response: %s",
                ((ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public RawlsRetryHandler(
      BackOffPolicy backoffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(backoffPolicy, new FirecloudRetryPolicy(), termsOfServiceApiProvider);
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return maybeConvertMessageForTos(exception.getCode())
        .orElseGet(() -> ExceptionUtils.convertFirecloudException(exception));
  }
}
