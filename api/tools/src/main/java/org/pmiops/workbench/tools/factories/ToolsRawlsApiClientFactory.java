package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import org.pmiops.workbench.rawls.ApiClient;
import org.pmiops.workbench.rawls.api.BillingV2Api;
import org.pmiops.workbench.rawls.api.WorkspacesApi;

public abstract class ToolsRawlsApiClientFactory {

  protected ApiClient apiClient;

  protected ToolsRawlsApiClientFactory(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  protected static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  public WorkspacesApi workspacesApi() throws IOException {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  public BillingV2Api billingV2Api() throws IOException {
    BillingV2Api api = new BillingV2Api();
    api.setApiClient(apiClient);
    return api;
  }
}
