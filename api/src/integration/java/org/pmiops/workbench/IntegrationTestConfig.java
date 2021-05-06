package org.pmiops.workbench;

import static org.pmiops.workbench.BaseIntegrationTest.loadTestConfig;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import java.io.IOException;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@TestConfiguration
@Import({RetryConfig.class, CommonConfig.class, CloudStorageClientImpl.class})
public class IntegrationTestConfig {

  @Bean(name = FireCloudConfig.END_USER_API_CLIENT)
  ApiClient endUserApiClient() {
    // Integration tests can't make calls using user credentials.
    return null;
  }

  /**
   * Returns the Apache HTTP transport. Compare to AppEngineConfig which returns the App Engine HTTP
   * transport.
   */
  @Bean
  HttpTransport httpTransport() {
    return new ApacheHttpTransport();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  WorkbenchConfig getIntegrationTestConfig() throws IOException {
    return loadTestConfig();
  }
}
