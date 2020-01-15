package org.pmiops.workbench.rdr;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;
import org.pmiops.workbench.auth.ServiceAccounts;


@org.springframework.context.annotation.Configuration
public class RdrExportConfig {

  private static final List<String> SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public RdrApi rdrApi(WorkbenchConfig workbenchConfig) {
    RdrApi api = new RdrApi();
    org.pmiops.workbench.rdr.ApiClient apiClient = new org.pmiops.workbench.rdr.ApiClient();
    apiClient.setDebugging(true);
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(SCOPES));
      apiClient.setBasePath("https://" + workbenchConfig.rdrExport.host);

    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    api.setApiClient(apiClient);
    return api;
  }
}
