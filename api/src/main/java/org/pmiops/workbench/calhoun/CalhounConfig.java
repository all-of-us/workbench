package org.pmiops.workbench.calhoun;

import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.calhoun.api.ConvertApi;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class CalhounConfig {
  public static final String END_USER_LENIENT_TIMEOUT_API_CLIENT =
      "calhounEndUserLenientTimeoutApiClient";
  public static final String END_USER_CALHOUN_API = "calhounEndUserApi";

  @Bean(name = END_USER_LENIENT_TIMEOUT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserLenientTimeoutApiClient(
      UserAuthentication userAuthentication,
      CalhounApiClientFactory factory,
      WorkbenchConfig config) {
    ApiClient apiClient = factory.newCalhounApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    apiClient
        .getHttpClient()
        .setReadTimeout(config.firecloud.lenientTimeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }

  @Bean(name = END_USER_CALHOUN_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ConvertApi endUserCalhounApi(
      @Qualifier(END_USER_LENIENT_TIMEOUT_API_CLIENT) ApiClient apiClient) {
    ConvertApi api = new ConvertApi();
    api.setApiClient(apiClient);
    return api;
  }
}
