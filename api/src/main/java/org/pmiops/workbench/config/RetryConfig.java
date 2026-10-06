package org.pmiops.workbench.config;

import org.pmiops.workbench.google.GoogleRetryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;

@Configuration
@Import({
  GoogleRetryHandler.class,
})
public class RetryConfig {

  @Bean
  public Sleeper sleeper() {
    return new ThreadWaitSleeper();
  }

  @Bean
  public BackOffPolicy backOffPolicy(Sleeper sleeper) {
    // Defaults to 100ms initial interval, doubling each time, with some random multiplier.
    ExponentialRandomBackOffPolicy policy = new ExponentialRandomBackOffPolicy();
    // Set max interval of 20 seconds.
    policy.setMaxInterval(20000);
    policy.setSleeper(sleeper);
    return policy;
  }
}
