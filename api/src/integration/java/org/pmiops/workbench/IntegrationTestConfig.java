package org.pmiops.workbench;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.google.GoogleConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@Import({RetryConfig.class, CommonConfig.class, CloudStorageClientImpl.class, GoogleConfig.class})
public class IntegrationTestConfig {

  public static final String END_USER_API_CLIENT = "endUserApiClient";

  @Bean(name = END_USER_API_CLIENT)
  @Primary
  ApiClient endUserApiClient() {
    // Integration tests can't make calls using user credentials.
    return null;
  }

  /**
   * Returns the Apache HTTP transport. Compare to AppEngineConfig which returns the App Engine HTTP
   * transport.
   *
   * @return
   */
  @Bean
  HttpTransport httpTransport() {
    return new ApacheHttpTransport();
  }
}
