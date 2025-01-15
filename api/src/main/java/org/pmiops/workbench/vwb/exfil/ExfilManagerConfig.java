package org.pmiops.workbench.vwb.exfil;

import java.io.IOException;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.vwb.common.VwbApiClientUtils;
import org.pmiops.workbench.vwb.exfil.api.EgressEventApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class ExfilManagerConfig {
  public static final String EXFIL_SERVICE_ACCOUNT_API_CLIENT = "EXFIL_SERVICE_ACCOUNT_API_CLIENT";

  @Bean(name = EXFIL_SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient apiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.vwb.exfilManagerBaseUrl);
    try {
      apiClient.setAccessToken(
          ServiceAccounts.getScopedServiceAccessToken(VwbApiClientUtils.SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public EgressEventApi egressEventApi(
      @Qualifier(EXFIL_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    return new EgressEventApi(apiClient);
  }
}
