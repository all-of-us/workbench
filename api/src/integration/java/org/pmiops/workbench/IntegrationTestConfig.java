package org.pmiops.workbench;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;

import java.io.IOException;
import java.nio.charset.Charset;

@TestConfiguration
@Import({RetryConfig.class, CommonConfig.class, CloudStorageServiceImpl.class})
public class IntegrationTestConfig {

  @Lazy
  @Bean(name = Constants.GSUITE_ADMIN_CREDS)
  ServiceAccountCredentials gsuiteAdminCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getGSuiteAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.FIRECLOUD_ADMIN_CREDS)
  ServiceAccountCredentials fireCloudCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getFireCloudAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
  ServiceAccountCredentials cloudResourceManagerCredentials(
      CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getCloudResourceManagerAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
  ServiceAccountCredentials defaultServiceAccountCredentials(
      CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getDefaultServiceAccountCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  WorkbenchConfig workbenchConfig() throws IOException {
    String testConfig =
        Resources.toString(Resources.getResource("config_test.json"), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }

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
