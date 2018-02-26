package org.pmiops.workbench.notebooks;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class NotebooksConfig {

  private static final Logger log = Logger.getLogger(NotebooksConfig.class.getName());

  private static final String NOTEBOOKS_CLIENT = "notebooksApiClient";

  @Bean(name=NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient notebooksApiClient(UserAuthentication userAuthentication,
      WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    // TODO: replace with whatever the actual creds for notebooks are
    apiClient.setAccessToken(userAuthentication.getCredentials());
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ClusterApi clusterApi(@Qualifier(NOTEBOOKS_CLIENT) ApiClient apiClient) {
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }
}
