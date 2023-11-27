package org.pmiops.workbench.terra;

import java.util.Optional;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

// a retry handler specifically for Terra services (e.g. Sam, Firecloud-Orchestration)
// and not appropriate for other services like Google
public abstract class TerraServiceRetryHandler<E extends Exception> extends RetryHandler<E> {

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
  protected Optional<WorkbenchException> maybeConvertMessageForTos(int errorCode) {
    if (errorCode != HttpServletResponse.SC_UNAUTHORIZED) {
      return Optional.empty();
    }

    // assume TOS non-compliance is the reason for 401/Unauth
    String exceptionMessage = "User has not accepted the Terra Terms of Service";
    ErrorCode code = ErrorCode.TERRA_TOS_NON_COMPLIANT;
    try {
      if (Boolean.TRUE.equals(termsOfServiceApiProvider.get().getTermsOfServiceStatus())) {
        // user is TOS-compliant, so the 401 is for some other reason; don't modify the exception
        return Optional.empty();
      }
    } catch (ApiException tosException) {
      exceptionMessage =
          "An exception was thrown checking the user's Terra Terms of Service Status: "
              + tosException.getMessage();
      code = ErrorCode.TERRA_TOS_COMPLIANCE_UNKNOWN;
    }

    return Optional.of(
        new UnauthorizedException(WorkbenchException.errorResponse(exceptionMessage, code)));
  }
}
