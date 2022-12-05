package org.pmiops.workbench.utils;

import java.util.Optional;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.model.ErrorCode;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

public abstract class RetryHandler<E extends Exception> {

  private final RetryTemplate retryTemplate;

  private static RetryTemplate retryTemplate(BackOffPolicy backOffPolicy, RetryPolicy retryPolicy) {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setThrowLastExceptionOnExhausted(true);
    return retryTemplate;
  }

  public RetryHandler(BackOffPolicy backOffPolicy, RetryPolicy retryPolicy) {
    this(retryTemplate(backOffPolicy, retryPolicy));
  }

  public RetryHandler(RetryTemplate retryTemplate) {
    this.retryTemplate = retryTemplate;
  }

  @SuppressWarnings("unchecked")
  public final <T> T run(RetryCallback<T, E> retryCallback) {
    try {
      return retryTemplate.execute(retryCallback);
    } catch (RetryException retryException) {
      throw new ServerErrorException(retryException.getCause());
    } catch (Exception exception) {
      throw convertException((E) exception);
    }
  }

  public final <T> T runAndThrowChecked(RetryCallback<T, E> retryCallback) throws E {
    return retryTemplate.execute(retryCallback);
  }

  private static final String TERMS_OF_SERVICE_NONCOMPLIANCE_MESSAGE =
      "User has not accepted the Terra Terms of Service";

  protected abstract WorkbenchException convertException(E exception);

  // ToS non-compliance causes Terra services to return 401/Unauth - but that's not the only
  // reason we might see 401 here.  Call Terra again to check ToS status.
  protected static Optional<WorkbenchException> checkForTosNonCompliance(
      Provider<TermsOfServiceApi> termsOfServiceApiProvider, int errorCode) {
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
}
