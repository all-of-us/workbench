package org.pmiops.workbench.google;

import com.google.auth.Credentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Provider;

@Configuration
public class StorageConfig {

    public static final String WGS_EXTRACTION_STORAGE = "WGS_EXTRACTION_STORAGE";
    public static final String WGS_EXTRACTION_STORAGE_CLIENT = "WGS_EXTRACTION_STORAGE_CLIENT";

    @Bean
    @Primary
    Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean(name = WGS_EXTRACTION_STORAGE)
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    Storage wgsExtractionStorage(@Qualifier(FireCloudConfig.WGS_EXTRACTION_SA_CREDENTIALS) Credentials credentials) {
        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean
    @Primary
    CloudStorageClient cloudStorageClient(Storage storage, Provider<WorkbenchConfig> configProvider) {
        return new CloudStorageClientImpl(storage, configProvider);
    }

    @Bean(name = WGS_EXTRACTION_STORAGE_CLIENT)
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    CloudStorageClient wgsExtractioncloudStorageClient(@Qualifier(WGS_EXTRACTION_STORAGE) Storage storage, Provider<WorkbenchConfig> configProvider) {
        return new CloudStorageClientImpl(storage, configProvider);
    }

}
