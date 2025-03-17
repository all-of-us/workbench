package org.pmiops.workbench.utils;

import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.pmiops.workbench.calhoun.api.ConvertApi;
import org.pmiops.workbench.config.WorkbenchConfig;

public class ApiClientUtils {
  public static ConvertApi withLenientTimeout(
      WorkbenchConfig workbenchConfig, ConvertApi convertApi) {
    convertApi.setApiClient(withLenientTimeout(workbenchConfig, convertApi.getApiClient()));
    return convertApi;
  }

  public static DisksApi withLenientTimeout(WorkbenchConfig workbenchConfig, DisksApi disksApi) {
    disksApi.setApiClient(withLenientTimeout(workbenchConfig, disksApi.getApiClient()));
    return disksApi;
  }

  private static org.pmiops.workbench.calhoun.ApiClient withLenientTimeout(
      WorkbenchConfig workbenchConfig, org.pmiops.workbench.calhoun.ApiClient apiClient) {
    return apiClient.setReadTimeout(workbenchConfig.firecloud.lenientTimeoutInSeconds * 1000);
  }

  private static org.broadinstitute.dsde.workbench.client.leonardo.ApiClient withLenientTimeout(
      WorkbenchConfig workbenchConfig,
      org.broadinstitute.dsde.workbench.client.leonardo.ApiClient apiClient) {
    return apiClient.setReadTimeout(workbenchConfig.firecloud.lenientTimeoutInSeconds * 1000);
  }
}
