package org.pmiops.workbench.utils;

import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
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
    } catch (WorkbenchException workbenchException) {
      // No need to convert here, just pass through.
      throw workbenchException;
    } catch (Exception exception) {
      throw convertException((E) exception);
    }
  }

  public final <T> T runAndThrowChecked(RetryCallback<T, E> retryCallback) throws E {
    return retryTemplate.execute(retryCallback);
  }

  protected abstract WorkbenchException convertException(E exception);
}
