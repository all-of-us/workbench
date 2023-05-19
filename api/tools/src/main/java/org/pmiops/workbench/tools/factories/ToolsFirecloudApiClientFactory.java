package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.MethodRepositoryApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;

public abstract class ToolsFirecloudApiClientFactory extends ToolsApiClientFactory {

  protected ApiClient apiClient;

  protected ToolsFirecloudApiClientFactory(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ProfileApi profileApi() throws IOException {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }

  public MethodRepositoryApi methodRepositoryApi() throws IOException {
    MethodRepositoryApi api = new MethodRepositoryApi();
    api.setApiClient(apiClient);
    return api;
  }
}
