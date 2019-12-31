package org.pmiops.workbench.tools;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import java.io.IOException;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.DbConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
 *
 * <p>IMPORTANT: This config should only be used in a database context. In order to implement a tool
 * which uses this config, that tool must be run with an active SQL server available, e.g. via Cloud
 * SQL Proxy.
 */
@Configuration
@EnableAutoConfiguration
@Import({RetryConfig.class, CommonConfig.class, CloudStorageServiceImpl.class})
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class CommandLineToolConfig {

  /**
   * Loads the GSuite admin service account key from GCS.
   *
   * <p>This needs to be annotated with @Lazy so only classes that use it (e.g. backfill scripts
   * which require a WorkbenchConfig instance) will trigger the file load attempt.
   *
   * <p>Any command-line tool which loads this bean needs to be called from a project.rb command
   * which is preceded with "get_gsuite_admin_key" to ensure the local key file si populated.
   */
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

  /**
   * Instead of using the CacheSpringConfiguration class (which has a request-scoped bean to return
   * a cached workbench config), we load the workbench config once from the database and use that.
   */
  @Bean
  @Lazy
  WorkbenchConfig workbenchConfig(ConfigDao configDao) {
    DbConfig config = configDao.findOne(DbConfig.MAIN_CONFIG_ID);

    Gson gson = new Gson();
    return gson.fromJson(config.getConfiguration(), WorkbenchConfig.class);
  }

  /**
   * Returns the Apache HTTP transport. Compare to CommonConfig which returns the App Engine HTTP
   * transport.
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

  /**
   * Run a command line spring boot application. The provided configuration should provide a
   * CommandLineRunner bean. The rest of the configuration is applied as a child to the standard
   * CommandLineToolConfig context, which provides common database / service access. The child
   * relationship allows the CLI config class to override parent beans if needed.
   *
   * @param cliConfig the class which declares the CommandLineRunner and optionally other
   *     dependencies
   * @param args command line args to pass to the program
   */
  public static void runCommandLine(Class<?> cliConfig, String[] args) {
    new SpringApplicationBuilder(CommandLineToolConfig.class).child(cliConfig).web(false).run(args);
  }
}
