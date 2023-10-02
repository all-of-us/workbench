package org.pmiops.workbench.wsm;

import bio.terra.workspace.client.ApiException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.TerraServiceRetryHandler;
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
      if (lastException instanceof org.pmiops.workbench.firecloud.ApiException) {
        return ((org.pmiops.workbench.firecloud.ApiException) lastException).getCode();
      }
      if (lastException instanceof SocketTimeoutException) {
        return HttpServletResponse.SC_GATEWAY_TIMEOUT;
      }
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    protected void logNoRetry(Throwable t, int responseCode) {
      if (t instanceof org.pmiops.workbench.firecloud.ApiException) {
        logger.log(
            getLogLevel(responseCode),
            String.format(
                "Exception calling WorkspaceManager API with response: %s",
                ((org.pmiops.workbench.firecloud.ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  public WsmRetryHandler(
      BackOffPolicy backOffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(backOffPolicy, new WsmRetryPolicy(), termsOfServiceApiProvider);
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return maybeConvertMessageForTos(exception.getCode())
        .orElseGet(() -> ExceptionUtils.convertWsmException(exception));
  }
}
