package org.pmiops.workbench.tools;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.gson.Gson;
import java.io.IOException;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;

/**
 * Contains Spring beans for dependencies which are different for classes run in the context of a
 * command-line tool versus a WebMVC request handler.
 *
 * <p>The main difference is that request-scoped dependencies cannot be used from a command-line
 * context.
 */
@Configuration
@EnableJpaRepositories({"org.pmiops.workbench"})
@Import({RetryConfig.class, CommonConfig.class})
// Scan the google module, for CloudStorageService and DirectoryService beans.
@ComponentScan("org.pmiops.workbench.google")
// Scan the FireCloud module, for FireCloudService bean.
@ComponentScan("org.pmiops.workbench.firecloud")
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
public class CommandLineToolConfig {

  /**
   * Loads the GSuite admin service account key from GCS.
   *
   * <p>This needs to be annotated with @Lazy so only classes that use it (e.g. backfill scripts
   * which require a WorkbenchConfig instance) will trigger the file load attempt.
   *
   * <p>Any command-line tool which loads this bean needs to be called from a project.rb command
   * which is preceded with "get_gsuite_admin_key" to ensure the local key file si populated.
   *
   * @return
   */
  @Lazy
  @Bean(name = Constants.GSUITE_ADMIN_CREDS)
  GoogleCredential gsuiteAdminCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getGSuiteAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.FIRECLOUD_ADMIN_CREDS)
  GoogleCredential fireCloudCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getFireCloudAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Lazy
  @Bean(name = Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
  GoogleCredential cloudResourceManagerCredentials(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getCloudResourceManagerAdminCredentials();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Instead of using the CacheSpringConfiguration class (which has a request-scoped bean to return
   * a cached workbench config), we load the workbench config once from the database and use that.
   *
   * @param configDao
   * @return
   */
  @Bean
  @Lazy
  WorkbenchConfig workbenchConfig(ConfigDao configDao) {
    Config config = configDao.findOne(Config.MAIN_CONFIG_ID);
    Gson gson = new Gson();
    return gson.fromJson(config.getConfiguration(), WorkbenchConfig.class);
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
