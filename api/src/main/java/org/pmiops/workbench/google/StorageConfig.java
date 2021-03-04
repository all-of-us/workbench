package org.pmiops.workbench.google;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Provider;

@Configuration
public class StorageConfig {

    public static final String WGS_EXTRACTION = "WGS_EXTRACTION";

    @Bean
    Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean(name = WGS_EXTRACTION)
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    Storage wgsExtractionStorage(@Qualifier(FireCloudConfig.WGS_EXTRACTION_SA_CREDENTIALS) Credentials credentials) {
        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @Bean
    CloudStorageService cloudStorageService(Storage storage, Provider<WorkbenchConfig> configProvider) {
        return new CloudStorageServiceImpl(storage, configProvider);
    }

    @Bean(name = WGS_EXTRACTION)
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    CloudStorageService wgsExtractionCloudStorageService(@Qualifier(WGS_EXTRACTION) Storage storage, Provider<WorkbenchConfig> configProvider) {
        return new CloudStorageServiceImpl(storage, configProvider);
    }

}
