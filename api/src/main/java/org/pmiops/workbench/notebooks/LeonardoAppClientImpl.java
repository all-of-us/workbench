package org.pmiops.workbench.notebooks;

import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.PersistentDiskRequest;
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
  private final LeonardoMapper leonardoMapper;
  private final LeonardoRetryHandler leonardoRetryHandler;

  @Autowired
  public LeonardoAppClientImpl(
      LeonardoApiClientFactory leonardoApiClientFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<AppsApi> appsApiProvider,
      FireCloudService fireCloudService,
      LeonardoMapper leonardoMapper,
      LeonardoRetryHandler leonardoRetryHandler) {
    this.leonardoApiClientFactory = leonardoApiClientFactory;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.appsApiProvider = appsApiProvider;
    this.fireCloudService = fireCloudService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoRetryHandler = leonardoRetryHandler;
  }

  public void createLeonardoApp(String googleProject, String name, LeonardoAppType appType)
      throws WorkbenchException, ApiException {
    AppsApi appsApi = appsApiProvider.get();
    LeonardoCreateAppRequest leonardoCreateAppRequest = new LeonardoCreateAppRequest().appType(appType).diskConfig(new LeonardoPersistentDiskRequest().diskType(
        LeonardoDiskType.STANDARD).size(100));
    if(! appType.equals(LeonardoAppType.CROMWELL)) {
      leonardoCreateAppRequest.setDescriptorPath("https://github.com/DataBiosphere/terra-app/blob/main/apps/rstudio/app.yaml");
    }
    appsApi.createApp(googleProject,  name, leonardoCreateAppRequest);
  }
}
