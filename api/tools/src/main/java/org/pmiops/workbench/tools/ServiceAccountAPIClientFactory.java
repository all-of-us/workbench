package org.pmiops.workbench.tools;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.protobuf.Duration;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.*;

public class ServiceAccountAPIClientFactory extends ApiClientFactory {

  public ServiceAccountAPIClientFactory(String apiUrl) {
    try {
      this.apiClient = newApiClient(apiUrl);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(this.FC_SCOPES)));
    return apiClient;
  }

}
