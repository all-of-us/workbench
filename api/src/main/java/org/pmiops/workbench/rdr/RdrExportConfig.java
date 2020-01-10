package org.pmiops.workbench.rdr;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class RdrExportConfig {

  private static final List<String> SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");

  private static final String BASE_PATH = "/rdr/v1";

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public RdrApi rdrApi(
      ServiceAccounts serviceAccounts,
      WorkbenchConfig workbenchConfig) {
    RdrApi api = new RdrApi();
    org.pmiops.workbench.rdr.ApiClient apiClient = new org.pmiops.workbench.rdr.ApiClient();
    apiClient.setDebugging(true);
    try {
      apiClient.setAccessToken(serviceAccounts.getScopedServiceAccessToken(SCOPES));
      apiClient.setBasePath("https://" + workbenchConfig.rdrExport.host + BASE_PATH);

    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    api.setApiClient(apiClient);
    return api;
  }
}
