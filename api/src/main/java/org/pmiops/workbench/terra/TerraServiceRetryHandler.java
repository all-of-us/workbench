package org.pmiops.workbench.terra;

import java.util.Optional;
import java.util.function.Function;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.utils.RetryHandler;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;

// a retry handler specifically for Terra services (e.g. Sam, Firecloud-Orchestration)
// and not appropriate for other services like Google
public abstract class TerraServiceRetryHandler<E extends Exception> extends RetryHandler<E> {

  private final FireCloudService fireCloudService;
  private final Function<E, WorkbenchException> defaultExceptionConverter;

  public TerraServiceRetryHandler(
      BackOffPolicy backOffPolicy,
      RetryPolicy retryPolicy,
      FireCloudService fireCloudService,
      Function<E, WorkbenchException> defaultExceptionConverter) {
    super(backOffPolicy, retryPolicy);
    this.fireCloudService = fireCloudService;
    this.defaultExceptionConverter = defaultExceptionConverter;
  }

  // ToS non-compliance causes Terra services to return 401/Unauth - but that's not the only
  // reason we might see 401 here.  If 401/SC_UNAUTHORIZED, call Terra again to check ToS status.
  private Optional<WorkbenchException> maybeConvertMessageForTos(int errorCode) {
    if (errorCode != HttpServletResponse.SC_UNAUTHORIZED
        || fireCloudService.isUserCompliantWithTerraToS()) {
      // not 401, or user is TOS-compliant so the 401 is for some other reason; don't modify the
      // exception
      return Optional.empty();
    } else {
      var error =
          WorkbenchException.errorResponse(
              "User has not accepted the Terra Terms of Service",
              ErrorCode.TERRA_TOS_NON_COMPLIANT);
      return Optional.of(new UnauthorizedException(error));
    }
  }

  protected WorkbenchException convertTerraException(E exception, int code) {
    return maybeConvertMessageForTos(code)
        .orElseGet(() -> defaultExceptionConverter.apply(exception));
  }
}
