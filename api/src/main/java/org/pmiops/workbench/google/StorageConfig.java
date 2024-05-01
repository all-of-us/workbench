package org.pmiops.workbench.google;

import com.google.auth.Credentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class StorageConfig {

  public static final String GENOMIC_EXTRACTION_STORAGE = "GENOMIC_EXTRACTION_STORAGE";
  public static final String GENOMIC_EXTRACTION_STORAGE_CLIENT =
      "GENOMIC_EXTRACTION_STORAGE_CLIENT";

  @Bean
  @Primary
  Storage storage() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean(name = GENOMIC_EXTRACTION_STORAGE)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  Storage genomicExtractionStorage(
      @Qualifier(FireCloudConfig.WGS_EXTRACTION_SA_CREDENTIALS) Credentials credentials) {
    return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
  }

  @Bean
  @Primary
  CloudStorageClient cloudStorageClient(
      Provider<Storage> storageProvider, Provider<WorkbenchConfig> configProvider) {
    return new CloudStorageClientImpl(storageProvider, configProvider);
  }

  @Bean(name = GENOMIC_EXTRACTION_STORAGE_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  CloudStorageClient genomicExtractioncloudStorageClient(
      @Qualifier(GENOMIC_EXTRACTION_STORAGE) Provider<Storage> storageProvider,
      Provider<WorkbenchConfig> configProvider) {
    return new CloudStorageClientImpl(storageProvider, configProvider);
  }
}
