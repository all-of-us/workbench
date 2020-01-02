package org.pmiops.workbench;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.StoredCredentialsConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;

@TestConfiguration
@Import({RetryConfig.class, CommonConfig.class, StoredCredentialsConfig.class})
public class IntegrationTestConfig {

  @Bean(name = FireCloudConfig.END_USER_API_CLIENT)
  ApiClient endUserApiClient() {
    // Integration tests can't make calls using user credentials.
    return null;
  }

  /**
   * Returns the Apache HTTP transport. Compare to CommonConfig which returns the App Engine HTTP
   * transport.
   *
   * @return
   */
  @Bean
  HttpTransport httpTransport() {
    return new ApacheHttpTransport();
  }

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
