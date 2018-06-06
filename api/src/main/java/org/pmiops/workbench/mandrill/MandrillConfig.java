package org.pmiops.workbench.mandrill;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class MandrillConfig{

  private static final String MANDRILL_API_CLIENT = "mandrillApiClient";

  @Bean(name=MANDRILL_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient mandrillApiClient(WorkbenchConfig workbenchConfig) {
    return new ApiClient();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public MandrillApi mandrillApi(@Qualifier(MANDRILL_API_CLIENT) ApiClient apiClient) {
    MandrillApi api = new MandrillApi();
    api.setApiClient(apiClient);
    return api;
  }
}