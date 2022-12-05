package org.pmiops.workbench.iam;

import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.sam.ApiException;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class SamRetryHandler extends RetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(SamRetryHandler.class.getName());

  private final Provider<TermsOfServiceApi> termsOfServiceApiProvider;

  private static class SamRetryPolicy extends ResponseCodeRetryPolicy {

    public SamRetryPolicy() {
      super("Sam API");
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
                "Exception calling Sam API with response: %s",
                ((ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public SamRetryHandler(
      BackOffPolicy backoffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(backoffPolicy, new SamRetryPolicy());
    this.termsOfServiceApiProvider = termsOfServiceApiProvider;
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    if (exception.getCode() == HttpServletResponse.SC_UNAUTHORIZED) {
      Optional<WorkbenchException> tosExceptionMaybe =
          checkForTosNonCompliance(termsOfServiceApiProvider);
      if (tosExceptionMaybe.isPresent()) {
        return tosExceptionMaybe.get();
      }
    }

    return ExceptionUtils.convertSamException(exception);
  }
}
