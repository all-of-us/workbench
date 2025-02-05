package org.pmiops.workbench.vwb.sam;

import java.io.IOException;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.vwb.common.VwbApiClientUtils;
import org.pmiops.workbench.vwb.sam.api.GroupApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class VwbSamConfig {
  public static final String SAM_SERVICE_ACCOUNT_API_CLIENT = "SAM_SERVICE_ACCOUNT_API_CLIENT";

  @Bean(name = SAM_SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient apiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.vwb.vwbSamBaseUrl);
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
  public GroupApi groupApi(@Qualifier(SAM_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    return new GroupApi(apiClient);
  }
}
