package org.pmiops.workbench.firecloud;

import java.net.SocketTimeoutException;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.utils.ResponseCodeRetryPolicy;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.stereotype.Service;

@Service
public class FirecloudRetryHandler extends RetryHandler<ApiException> {

  private static final Logger logger = Logger.getLogger(FirecloudRetryHandler.class.getName());
  private static final String TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE =
      "User has not accepted the Terra Terms of Service";

  private final Provider<TermsOfServiceApi> termsOfServiceApiProvider;

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
                "Exception calling Firecloud API with response: %s",
                ((ApiException) t).getResponseBody()),
            t);
      } else {
        super.logNoRetry(t, responseCode);
      }
    }
  }

  @Autowired
  public FirecloudRetryHandler(
      BackOffPolicy backoffPolicy, Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(backoffPolicy, new FirecloudRetryPolicy());
    this.termsOfServiceApiProvider = termsOfServiceApiProvider;
  }

  @Override
  protected WorkbenchException convertException(ApiException exception) {
    Function<ApiException, WorkbenchException> defaultHandler =
        ExceptionUtils::convertFirecloudException;

    if (exception.getCode() == HttpServletResponse.SC_UNAUTHORIZED) {
      return checkToSCompliance(exception, defaultHandler);
    }

    return defaultHandler.apply(exception);
  }

  // ToS non-compliance causes Firecloud to return 401/Unauth - but that's not the only
  // reason we might see 401 here.  Call Firecloud again to check ToS status.
  private WorkbenchException checkToSCompliance(
      ApiException exception, Function<ApiException, WorkbenchException> defaultHandler) {
    boolean tosCompliant = false;
    String tosExceptionMessage = TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE;

    try {
      tosCompliant = Boolean.TRUE.equals(termsOfServiceApiProvider.get().getTermsOfServiceStatus());
    } catch (ApiException tosException) {
      tosExceptionMessage =
          "An exception was thrown checking the user's Terra Terms of Service Status: "
              + tosException.getMessage();
    }

    return tosCompliant
        ? defaultHandler.apply(exception)
        : new UnauthorizedException(
            WorkbenchException.errorResponse(
                tosExceptionMessage, ErrorCode.TERRA_TOS_NON_COMPLIANT));
  }
}
