package org.pmiops.workbench.vwb.wsm.wsm;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletResponse;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.terra.TerraServiceRetryHandler;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.wsmanager.ApiException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class WsmRetryHandler extends TerraServiceRetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(WsmRetryHandler.class.getName());

  private static class WsmRetryPolicy extends ResponseCodeRetryPolicy {

    public WsmRetryPolicy() {
      super("WorkspaceManager API");
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
                "Exception calling WorkspaceManager API with response: %s",
                ((ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  public WsmRetryHandler(
      BackOffPolicy backOffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(
        backOffPolicy,
        new WsmRetryPolicy(),
        termsOfServiceApiProvider,
        ExceptionUtils::convertWsmException);
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return convertTerraException(exception, exception.getCode());
  }
}
