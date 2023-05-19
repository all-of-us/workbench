package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import org.pmiops.workbench.rawls.ApiClient;

public class ToolsRawlsImpersonatedServiceAccountApiClientFactory
    extends ToolsRawlsApiClientFactory {

  public ToolsRawlsImpersonatedServiceAccountApiClientFactory(
      String targetServiceAccount, String rawlsBaseUrl) throws IOException {
    super(newApiClient(targetServiceAccount, rawlsBaseUrl));
  }

  private static ApiClient newApiClient(String targetServiceAccount, String rawlsBaseUrl)
      throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(rawlsBaseUrl);
    apiClient.setAccessToken(getAccessToken(targetServiceAccount));

    return apiClient;
  }
}
