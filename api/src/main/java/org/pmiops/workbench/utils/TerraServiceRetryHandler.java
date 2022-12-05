package org.pmiops.workbench.utils;

import java.util.Optional;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.model.ErrorCode;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

// a retry handler specifically for Terra services (e.g. Sam, Firecloud-Orchestration)
// and not appropriate for other services like Google
public abstract class TerraServiceRetryHandler<E extends Exception> extends RetryHandler<E> {
  private static final String TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE =
      "User has not accepted the Terra Terms of Service";

  private final Provider<TermsOfServiceApi> termsOfServiceApiProvider;

  public TerraServiceRetryHandler(
      BackOffPolicy backOffPolicy,
      RetryPolicy retryPolicy,
      Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    super(backOffPolicy, retryPolicy);
    this.termsOfServiceApiProvider = termsOfServiceApiProvider;
  }

  // ToS non-compliance causes Terra services to return 401/Unauth - but that's not the only
  // reason we might see 401 here.  Call Terra again to check ToS status.
  protected Optional<WorkbenchException> maybeTosNonCompliant(int errorCode) {
    if (errorCode == HttpServletResponse.SC_UNAUTHORIZED) {
      boolean tosCompliant = false;
      String tosExceptionMessage = TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE;

      try {
        tosCompliant =
            Boolean.TRUE.equals(termsOfServiceApiProvider.get().getTermsOfServiceStatus());
      } catch (ApiException tosException) {
        tosExceptionMessage =
            "An exception was thrown checking the user's Terra Terms of Service Status: "
                + tosException.getMessage();
      }

      if (!tosCompliant) {
        return Optional.of(
            new UnauthorizedException(
                WorkbenchException.errorResponse(
                    tosExceptionMessage, ErrorCode.TERRA_TOS_NON_COMPLIANT)));
      }
    }

    return Optional.empty();
  }

  protected Optional<WorkbenchException> maybeTosNonCompliantX() {
    boolean tosCompliant = false;
    String tosExceptionMessage = TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE;

    try {
      tosCompliant = Boolean.TRUE.equals(termsOfServiceApiProvider.get().getTermsOfServiceStatus());
    } catch (ApiException tosException) {
      tosExceptionMessage =
          "An exception was thrown checking the user's Terra Terms of Service Status: "
              + tosException.getMessage();
    }

    if (!tosCompliant) {
      return Optional.of(
          new UnauthorizedException(
              WorkbenchException.errorResponse(
                  tosExceptionMessage, ErrorCode.TERRA_TOS_NON_COMPLIANT)));
    }

    return Optional.empty();
  }
}
