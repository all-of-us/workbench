package org.pmiops.workbench.vwb.usermanager;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rawls.RawlsApiClientFactory;
import org.pmiops.workbench.vwb.user.ApiClient;
import org.pmiops.workbench.vwb.user.api.OrganizationV2Api;
import org.pmiops.workbench.vwb.user.api.UserV2Api;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class VwbUserManagerConfig {

  public static final String VWB_SERVICE_ACCOUNT_USER_API_CLIENT =
      "VWB_SERVICE_ACCOUNT_USER_API_CLIENT";
  public static final String VWB_SERVICE_ACCOUNT_USER_API = "VWB_SERVICE_ACCOUNT_USER_API";
  public static final String VWB_SERVICE_ACCOUNT_ORGANIZATION_API =
      "VWB_SERVICE_ACCOUNT_ORGANIZATION_API";

  public static final List<String> SCOPES =
      ImmutableList.<String>builder().addAll(RawlsApiClientFactory.SCOPES).build();

  /**
   * Creates a User Manager API client, unauthenticated. Most clients should use an authenticated,
   * request scoped bean instead of calling this directly.
   */
  private ApiClient newApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.vwb.userManagerBaseUrl);
    apiClient.setReadTimeout(30000);
    apiClient.setConnectTimeout(30000);
    apiClient.setWriteTimeout(30000);
    return apiClient;
  }

  @Bean(name = VWB_SERVICE_ACCOUNT_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient serviceAccountApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = newApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = VWB_SERVICE_ACCOUNT_ORGANIZATION_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public OrganizationV2Api serviceAccountOrganizationV2Api(
      @Qualifier(VWB_SERVICE_ACCOUNT_USER_API_CLIENT) ApiClient apiClient) {
    return new OrganizationV2Api(apiClient);
  }

  @Bean(name = VWB_SERVICE_ACCOUNT_USER_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public UserV2Api serviceAccountUserV2Api(
      @Qualifier(VWB_SERVICE_ACCOUNT_USER_API_CLIENT) ApiClient apiClient) {
    return new UserV2Api(apiClient);
  }
}
