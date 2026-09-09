package org.pmiops.workbench.vwb.sam;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletResponse;
import java.net.SocketTimeoutException;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.terra.TerraServiceRetryHandler;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class VwbSamRetryHandler extends TerraServiceRetryHandler<ApiException> {

  private static class SamManagerRetryPolicy extends ResponseCodeRetryPolicy {

    public SamManagerRetryPolicy() {
      super("VWB Sam Manager API");
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
    protected String getResponseBody(Throwable lastException) {
      return lastException instanceof ApiException apiException
          ? apiException.getResponseBody()
          : null;
    }
  }

  public VwbSamRetryHandler(
      BackOffPolicy backOffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(
        backOffPolicy,
        new SamManagerRetryPolicy(),
        termsOfServiceApiProvider,
        ExceptionUtils::convertVwbSamException);
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return convertTerraException(exception, exception.getCode());
  }
}
