package org.pmiops.workbench.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class RetryHandlerTest {

  // Typed as IllegalArgumentException to exercise the bridge method that caused the
  // original ClassCastException: when a WorkbenchException propagates out, the erased
  // bridge method would cast it to IllegalArgumentException before Fix 1.
  private static final RetryHandler<IllegalArgumentException> handler = buildHandler();

  private static RetryHandler<IllegalArgumentException> buildHandler() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
    retryTemplate.setThrowLastExceptionOnExhausted(true);
    return new RetryHandler<>(retryTemplate) {
      @Override
      protected WorkbenchException convertException(IllegalArgumentException exception) {
        return new WorkbenchException(exception);
      }
    };
  }

  @Test
  public void run_propagatesWorkbenchException_withoutClassCastException() {
    ForbiddenException toThrow = new ForbiddenException("forbidden");
    // Before Fix 1: the bridge method cast ForbiddenException → IllegalArgumentException,
    // resulting in ClassCastException. After Fix 1: ForbiddenException propagates directly.
    assertThrows(
        ForbiddenException.class,
        () ->
            handler.run(
                context -> {
                  throw toThrow;
                }));
  }
}
