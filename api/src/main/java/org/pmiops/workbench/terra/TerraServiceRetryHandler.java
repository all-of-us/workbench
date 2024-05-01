package org.pmiops.workbench.terra;

import java.util.Optional;
import java.util.function.Function;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

// a retry handler specifically for Terra services (e.g. Sam, Firecloud-Orchestration)
// and not appropriate for other services like Google
public abstract class TerraServiceRetryHandler<E extends Exception> extends RetryHandler<E> {

  private final Provider<TermsOfServiceApi> termsOfServiceApiProvider;
  private final Function<E, WorkbenchException> defaultExceptionConverter;

  public TerraServiceRetryHandler(
      BackOffPolicy backOffPolicy,
      RetryPolicy retryPolicy,
      Provider<TermsOfServiceApi> termsOfServiceApiProvider,
      Function<E, WorkbenchException> defaultExceptionConverter) {
    super(backOffPolicy, retryPolicy);
    this.termsOfServiceApiProvider = termsOfServiceApiProvider;
    this.defaultExceptionConverter = defaultExceptionConverter;
  }

  // ToS non-compliance causes Terra services to return 401/Unauth - but that's not the only
  // reason we might see 401 here.  If 401/SC_UNAUTHORIZED, call Terra again to check ToS status.
  private Optional<WorkbenchException> maybeConvertMessageForTos(int errorCode) {
    if (errorCode != HttpServletResponse.SC_UNAUTHORIZED) {
      return Optional.empty();
    }

    // assume TOS non-compliance is the reason for 401/Unauth
    String exceptionMessage = "User has not accepted the Terra Terms of Service";
    ErrorCode code = ErrorCode.TERRA_TOS_NON_COMPLIANT;
    try {
      if (termsOfServiceApiProvider.get().userTermsOfServiceGetSelf().getPermitsSystemUsage()) {
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

  protected WorkbenchException convertTerraException(E exception, int code) {
    return maybeConvertMessageForTos(code)
        .orElseGet(() -> defaultExceptionConverter.apply(exception));
  }
}
