package org.pmiops.workbench.shibboleth;

import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.shibboleth.api.ShibbolethApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class ShibbolethConfig {
  public static final String X_APP_ID_HEADER = "X-App-ID";

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ShibbolethApi shibbolethApi(
      UserAuthentication userAuthentication, WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.firecloud.shibbolethApiBaseUrl);
    apiClient.addDefaultHeader(X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    apiClient.setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds * 1000);
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return new ShibbolethApi(apiClient);
  }
}
