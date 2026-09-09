package org.pmiops.workbench.vwb.usermanager;

import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletResponse;
import java.net.SocketTimeoutException;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.terra.TerraServiceRetryHandler;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.vwb.user.ApiException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class VwbUserManagerRetryHandler extends TerraServiceRetryHandler<ApiException> {

  private static class UserManagerRetryPolicy extends ResponseCodeRetryPolicy {

    public UserManagerRetryPolicy() {
      super("User Manager API");
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

    /**
     * User Manager returns some errors with no body at all. Deleting a notification that is already
     * inactive, for example, is a bare 404, so logging the body alone says nothing about what
     * failed. Fall back to the exception message and say the body was empty rather than printing a
     * blank.
     */
    private static String describeResponse(ApiException apiException) {
      String responseBody = apiException.getResponseBody();
      if (responseBody != null && !responseBody.isBlank()) {
        return responseBody;
      }
      return String.format("<empty body> (%s)", apiException.getMessage());
    }
  }

  public VwbUserManagerRetryHandler(
      BackOffPolicy backOffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(
        backOffPolicy,
        new UserManagerRetryPolicy(),
        termsOfServiceApiProvider,
        ExceptionUtils::convertUserManagerException);
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    return convertTerraException(exception, exception.getCode());
  }
}
