package org.pmiops.workbench.notebooks;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeonardoAppClientImpl implements LeonardoAppClient {
  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final LeonardoApiClientFactory leonardoApiClientFactory;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<AppsApi> appsApiProvider;
  private final FireCloudService fireCloudService;
  private final WorkspaceDao workspaceDao;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoRetryHandler leonardoRetryHandler;

  @Autowired
  public LeonardoAppClientImpl(
      LeonardoApiClientFactory leonardoApiClientFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<AppsApi> appsApiProvider,
      FireCloudService fireCloudService,
      WorkspaceDao workspaceDao, LeonardoMapper leonardoMapper,
      LeonardoRetryHandler leonardoRetryHandler) {
    this.leonardoApiClientFactory = leonardoApiClientFactory;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.appsApiProvider = appsApiProvider;
    this.fireCloudService = fireCloudService;
    this.workspaceDao = workspaceDao;
    this.leonardoMapper = leonardoMapper;
    this.leonardoRetryHandler = leonardoRetryHandler;
  }

  public void createLeonardoApp(String googleProject, String name, LeonardoAppType appType)
      throws WorkbenchException, ApiException {
    DbWorkspace workspace = workspaceDao.getByGoogleProject(googleProject).get();
    AppsApi appsApi = appsApiProvider.get();
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    customEnvironmentVariables.put("WORKSPACE_NAME", workspace.getName());
    customEnvironmentVariables.put("WORKSPACE_NAMESPACE", workspace.getName());
    customEnvironmentVariables.put("WORKSPACE_BUCKET", "bucket");
    customEnvironmentVariables.put("GOOGLE_PROJECT", workspace.getGoogleProject());
    LeonardoCreateAppRequest leonardoCreateAppRequest =
        new LeonardoCreateAppRequest()
            .appType(appType)
            .customEnvironmentVariables(customEnvironmentVariables)
            .diskConfig(
                new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.STANDARD).size(500).name("yonghao-disk-2"));
    if (!appType.equals(LeonardoAppType.CROMWELL)) {
      leonardoCreateAppRequest.setDescriptorPath(
          "https://github.com/DataBiosphere/terra-app/blob/main/apps/rstudio/app.yaml");
    }
    System.out.println("~~~22222");
    System.out.println("~~~22222");
    System.out.println(googleProject);
    appsApi.createApp(googleProject, "aou-test-" + name.toLowerCase(), leonardoCreateAppRequest);
  }
}
