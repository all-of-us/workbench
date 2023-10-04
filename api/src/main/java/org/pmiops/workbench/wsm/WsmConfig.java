package org.pmiops.workbench.wsm;

import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class WsmConfig {

  public static final String WSM_API_CLIENT = "WSM_API_CLIENT";
  public static final String WSM_WORKSPACE_CLIENT = "WSM_WORKSPACE_CLIENT";

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ControlledAwsResourceApi controlledAwsResourceApi(
      @Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new ControlledAwsResourceApi(apiClient);
  }

  @Bean(name = WSM_WORKSPACE_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspaceApi endUserWorkspaceClient(@Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new WorkspaceApi(apiClient);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ResourceApi resourceApi(@Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new ResourceApi(apiClient);
  }

  @Bean(name = WSM_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      HttpServletRequest request,
      UserAuthentication userAuthentication,
      GoogleCredentials credentials)
      throws ApiException {
    ApiClient apiClient = newApiClient();
    if (request.getHeader("authorization") != null) {
      String bearerToken = request.getHeader("authorization");
      if (StringUtils.isEmpty(bearerToken)) {
        throw new ApiException("Unauthenticated requests are not allowed");
      }
      bearerToken = bearerToken.split("Bearer ")[1];

      // FIXME read token from file just for now...
      try {
        bearerToken = Files.readString(Path.of("/tmp/bearer_token.txt"), Charset.defaultCharset());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      apiClient.setAccessToken(bearerToken.trim());
    } else {
      apiClient.setAccessToken(userAuthentication.getCredentials());
    }
    boolean useGoogleCredentials = false;
    if (useGoogleCredentials)
      apiClient.setAccessToken(credentials.getAccessToken().getTokenValue());
    return apiClient;
  }

  /**
   * Creates a WSM API client, unauthenticated. Most clients should use an authenticated, request
   * scoped bean instead of calling this directly.
   */
  private ApiClient newApiClient() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("https://terra-devel-wsm.api.verily.com");
    return apiClient;
  }
}
