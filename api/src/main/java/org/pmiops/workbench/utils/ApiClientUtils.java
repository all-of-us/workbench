package org.pmiops.workbench.utils;

import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.pmiops.workbench.calhoun.api.ConvertApi;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.rawls.api.WorkspacesApi;

public class ApiClientUtils {
  private ApiClientUtils() {}

  public static int getLenientTimeoutMillis(WorkbenchConfig workbenchConfig) {
    return workbenchConfig.firecloud.lenientTimeoutInSeconds * 1000;
  }

  // use lenient timeout in Calhoun's ConvertApi
  // and replace the existing client with a more standard one

  public static ConvertApi withLenientTimeout(
      WorkbenchConfig workbenchConfig, ConvertApi convertApi) {
    convertApi.setApiClient(withLenientTimeout(workbenchConfig, convertApi.getApiClient()));
    return convertApi;
  }

  private static org.pmiops.workbench.calhoun.ApiClient withLenientTimeout(
      WorkbenchConfig workbenchConfig, org.pmiops.workbench.calhoun.ApiClient apiClient) {
    return apiClient.setReadTimeout(getLenientTimeoutMillis(workbenchConfig));
  }

  // use lenient timeout in Rawls's WorkspacesApi
  // and remove the lenient-timeout client

  public static WorkspacesApi withLenientTimeout(
      WorkbenchConfig workbenchConfig, WorkspacesApi workspacesApi) {
    workspacesApi.setApiClient(withLenientTimeout(workbenchConfig, workspacesApi.getApiClient()));
    return workspacesApi;
  }

  private static org.pmiops.workbench.rawls.ApiClient withLenientTimeout(
      WorkbenchConfig workbenchConfig, org.pmiops.workbench.rawls.ApiClient apiClient) {
    return apiClient.setReadTimeout(getLenientTimeoutMillis(workbenchConfig));
  }

  // use an arbitrary timeout in Leonardo's DisksApi

  public static DisksApi withTimeoutInSeconds(int timeoutSeconds, DisksApi disksApi) {
    disksApi.setApiClient(withTimeoutInSeconds(timeoutSeconds, disksApi.getApiClient()));
    return disksApi;
  }

  private static org.broadinstitute.dsde.workbench.client.leonardo.ApiClient withTimeoutInSeconds(
      int timeoutSeconds, org.broadinstitute.dsde.workbench.client.leonardo.ApiClient apiClient) {
    return apiClient.setReadTimeout(timeoutSeconds);
  }
}
