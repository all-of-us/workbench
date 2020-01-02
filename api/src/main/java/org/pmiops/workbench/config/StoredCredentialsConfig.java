package org.pmiops.workbench.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Configuration
@Import(CloudStorageServiceImpl.class)
public class StoredCredentialsConfig {
  /**
   * Service account credentials for Gsuite administration, corresponding to the "gsuite-admin"
   * service account in each environment. Enabled for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.GSUITE_ADMIN_CREDS)
  public ServiceAccountCredentials gsuiteAdminCredential(CloudStorageService cloudStorageService)
      throws IOException {
    return cloudStorageService.getGSuiteAdminCredentials();
  }

  /**
   * Service account credentials for FireCloud administration. This Service Account has been enabled
   * for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.FIRECLOUD_ADMIN_CREDS)
  public ServiceAccountCredentials firecloudAdminCredential(CloudStorageService cloudStorageService)
      throws IOException {
    return cloudStorageService.getFireCloudAdminCredentials();
  }

  /**
   * Service account credentials for Cloud Resource Manager administration. This Service Account has
   * been enabled for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
  public ServiceAccountCredentials cloudResourceManagerAdminCredential(
      CloudStorageService cloudStorageService) throws IOException {
    return cloudStorageService.getCloudResourceManagerAdminCredentials();
  }
}
