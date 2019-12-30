package org.pmiops.workbench;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;

@Configuration
@Import({RetryConfig.class, CommonConfig.class})
// Scan the google package, which we need for the CloudStorage bean.
@ComponentScan("org.pmiops.workbench.google")
// Scan the ServiceAccounts class, but exclude other classes in auth (since they
// bring in JPA-related beans, which include a whole bunch of other deps that are
// more complicated than we need for now).
//
// TODO(gjuggler): move ServiceAccounts out of the auth package, or move the more
// dependency-ridden classes (e.g. ProfileService) out instead.
@ComponentScan(
    basePackageClasses = ServiceAccounts.class,
    useDefaultFilters = false,
    includeFilters = {
      @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = ServiceAccounts.class),
    })
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
  ServiceAccountCredentials cloudResourceManagerCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getCloudResourceManagerAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
  ServiceAccountCredentials defaultServiceAccountCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getDefaultServiceAccountCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  @Lazy
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
