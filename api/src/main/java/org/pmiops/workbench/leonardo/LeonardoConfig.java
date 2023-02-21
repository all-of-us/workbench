package org.pmiops.workbench.leonardo;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class LeonardoConfig {
  public static final String USER_RUNTIMES_API = "userRuntimesApi";
  public static final String SERVICE_RUNTIMES_API = "svcRuntimesApi";

  public static final String USER_DISKS_API = "userDisksApi";
  public static final String SERVICE_DISKS_API = "svcDisksApi";

  public static final String USER_APPS_API = "userAppsApi";

  public static final String SERVICE_APPS_API = "serviceAppsApi";

  // Identifiers for the Swagger2 APIs for Jupyter and Welder, used for creating/localizing files.
  private static final String USER_NOTEBOOKS_CLIENT = "notebooksApiClient";
  private static final String SERVICE_NOTEBOOKS_CLIENT = "notebooksSvcApiClient";
  // Identifiers for the new OAS3 APIs from Leonardo. These should be used for runtimes access.
  private static final String USER_LEONARDO_CLIENT = "leonardoApiClient";
  private static final String SERVICE_LEONARDO_CLIENT = "leonardoServiceAPiClient";

  private static final Logger log = Logger.getLogger(LeonardoConfig.class.getName());

  private static final List<String> NOTEBOOK_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");

  @Bean(name = USER_NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.notebooks.ApiClient notebooksApiClient(
      UserAuthentication userAuthentication,
      LeonardoApiClientFactory factory,
      HttpServletRequest req) {
    org.pmiops.workbench.notebooks.ApiClient apiClient = factory.newNotebooksClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());

    // We pass-through the "Referer" header to outgoing Proxy API requests. Leonardo verifies this
    // Header for all incoming traffic. See IA-2469, RW-6412.
    // Note that injecting @RequestHeader rather than the full request would be preferred, but
    // cannot be injected outside a Spring controller method.
    Optional<String> referer = Optional.ofNullable(req.getHeader("Referer"));
    if (referer.isPresent()) {
      apiClient.addDefaultHeader("Referer", referer.get());
    } else {
      log.info("no Referer request header found, requests to the Leo proxy API may be rejected");
    }
    return apiClient;
  }

  @Bean(name = SERVICE_LEONARDO_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.leonardo.ApiClient leoServiceApiClient(
      LeonardoApiClientFactory factory) {
    org.pmiops.workbench.leonardo.ApiClient apiClient = factory.newApiClient();
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(NOTEBOOK_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = USER_LEONARDO_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.leonardo.ApiClient leoUserApiClient(
      UserAuthentication userAuthentication, LeonardoApiClientFactory factory) {
    org.pmiops.workbench.leonardo.ApiClient apiClient = factory.newApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = SERVICE_NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.notebooks.ApiClient workbenchServiceAccountClient(
      LeonardoApiClientFactory factory) {
    org.pmiops.workbench.notebooks.ApiClient apiClient = factory.newNotebooksClient();
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(NOTEBOOK_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = USER_RUNTIMES_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public RuntimesApi runtimesApi(
      @Qualifier(USER_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    RuntimesApi api = new RuntimesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = USER_DISKS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public DisksApi disksApi(
      @Qualifier(USER_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    DisksApi api = new DisksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_DISKS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public DisksApi serviceDisksApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    DisksApi api = new DisksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProxyApi proxyApi(
      @Qualifier(USER_NOTEBOOKS_CLIENT) org.pmiops.workbench.notebooks.ApiClient apiClient) {
    ProxyApi api = new ProxyApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public JupyterApi jupyterApi(
      @Qualifier(USER_NOTEBOOKS_CLIENT) org.pmiops.workbench.notebooks.ApiClient apiClient) {
    JupyterApi api = new JupyterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_RUNTIMES_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public RuntimesApi serviceRuntimesApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    RuntimesApi api = new RuntimesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ServiceInfoApi serviceInfoApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    ServiceInfoApi api = new ServiceInfoApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = USER_APPS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public AppsApi appsApi(
      @Qualifier(USER_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    AppsApi api = new AppsApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_APPS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public AppsApi serviceAppsApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    AppsApi api = new AppsApi();
    api.setApiClient(apiClient);
    return api;
  }
}
