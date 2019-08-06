package org.pmiops.workbench.mandrill;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class MandrillConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient mandrillApiClient(WorkbenchConfig workbenchConfig) {
    return new ApiClient();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public MandrillApi mandrillApi(ApiClient apiClient) {
    MandrillApi api = new MandrillApi();
    api.setApiClient(apiClient);
    return api;
  }
}
