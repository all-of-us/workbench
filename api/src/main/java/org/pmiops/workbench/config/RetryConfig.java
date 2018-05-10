package org.pmiops.workbench.config;

import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.*;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

  public static abstract class ResponseCodeRetryPolicy
      extends SimpleRetryPolicy {
    private int minRetryCode = 501;
    private int maxRetryCode = 599;

    public void setMinRetryCode(int minRetryCode) {
      this.minRetryCode = minRetryCode;
    }

    public void setMaxRetryCode(int maxRetryCode) {
      this.maxRetryCode = maxRetryCode;
    }

    @Override
    public boolean canRetry(RetryContext context) {
      if (!super.canRetry(context)) {
        return false;
      }
      Throwable lastException = context.getLastThrowable();
      int code = getResponseCode(lastException);
      return code >= minRetryCode && code <= maxRetryCode;
    }

    protected abstract int getResponseCode(Throwable lastException);
  }

  @Bean
  public Sleeper sleeper() {
    return new ThreadWaitSleeper();
  }

  @Bean
  public BackOffPolicy backOffPolicy(Sleeper sleeper) {
    // Defaults to 100ms initial interval, 30 second timeout, doubling each time, with some random multiplier.
    ExponentialRandomBackOffPolicy policy = new ExponentialRandomBackOffPolicy();
    policy.setSleeper(sleeper);
    return policy;
  }
}
