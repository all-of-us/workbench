package org.pmiops.workbench.google;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

  @Bean
  @Primary
  Storage storage() {
    return StorageOptions.getDefaultInstance().getService();
  }

  @Bean
  @Primary
  CloudStorageClient cloudStorageClient(
      Provider<Storage> storageProvider, Provider<WorkbenchConfig> configProvider) {
    return new CloudStorageClientImpl(storageProvider, configProvider);
  }
}
