package org.pmiops.workbench.tanagra;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class TanagraConfig {

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TanagraApi tanagraApi(WorkbenchConfig workbenchConfig) {
        TanagraApi api = new TanagraApi();
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(workbenchConfig.tanagra.baseUrl);
        api.setApiClient(apiClient);
        return api;
    }
}
