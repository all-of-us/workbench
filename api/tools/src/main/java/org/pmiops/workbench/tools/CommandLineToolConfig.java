package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.gson.Gson;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;

/**
 * Contains Spring beans for dependencies which are different for classes run in the context of a
 * command-line tool versus a WebMVC request handler.
 * <p>
 * The main difference is that request-scoped dependencies cannot be used from a command-line
 * context.
 */
@Configuration
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
// Scan the google module, which contains the CloudStorageService bean.
@ComponentScan("org.pmiops.workbench.google")
public class CommandLineToolConfig {

  /**
   * Loads the GSuite admin service account key from GCS.
   *
   * This needs to be annotated with @Lazy so only classes that use it (e.g. BackfillGSuiteUserData
   * which requires a WorkbenchConfig instance) will trigger the file load attempt.
   *
   * Any command-line tool which loads this bean needs to be called from a project.rb command
   * which is preceded with "get_gsuite_admin_key" to ensure the local key file si populated.
   *
   * @return
   */
  @Lazy
  @Bean
  GoogleCredential googleCredential(CloudStorageService cloudStorageService) {
    try {
      return cloudStorageService.getGSuiteAdminCredentials();
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
   * Returns the Apache HTTP transport. Compare to CommonConfig which returns the App Engine
   * HTTP transport.
   * @return
   */
  @Bean
  HttpTransport httpTransport() {
    return new ApacheHttpTransport();
  }
}
