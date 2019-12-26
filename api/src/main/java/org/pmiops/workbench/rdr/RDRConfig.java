package org.pmiops.workbench.rdr;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rdr.api.RDRApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class RDRConfig {

  private static final List<String> SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing");

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public RDRApi rdrApi(
      ServiceAccounts serviceAccounts,
      WorkbenchEnvironment workbenchEnvironment,
      WorkbenchConfig workbenchConfig) {
    RDRApi api = new RDRApi();
    org.pmiops.workbench.rdr.ApiClient apiClient = new org.pmiops.workbench.rdr.ApiClient();
    apiClient.setDebugging(true);
    try {
      apiClient.setAccessToken(serviceAccounts.workbenchAccessToken(workbenchEnvironment, SCOPES));
      // Todo change to host in config
      apiClient.setBasePath("https://" + workbenchConfig.rdrServer.host + "/rdr/v1");

    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    api.setApiClient(apiClient);
    return api;
  }
}
