package org.pmiops.workbench.notebooks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest.WelderRegistryEnum;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoUserJupyterExtensionConfig;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoNotebooksClientImpl implements LeonardoNotebooksClient {

  private static final String WORKSPACE_CDR_ENV_KEY = "WORKSPACE_CDR";
  private static final String JUPYTER_DEBUG_LOGGING_ENV_KEY = "JUPYTER_DEBUG_LOGGING";
  private static final String MERGED_WGS_BUCKET_KEY = "MERGED_WGS_BUCKET";
  private static final String SINGLE_SAMPLE_ARRAY_BUCKET_KEY = "SINGLE_SAMPLE_ARRAY_BUCKET";

  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;
  private final Provider<ProxyApi> proxyApiProvider;
  private final Provider<ServiceInfoApi> serviceInfoApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final NotebooksRetryHandler notebooksRetryHandler;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoRetryHandler leonardoRetryHandler;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public LeonardoNotebooksClientImpl(
      @Qualifier(NotebooksConfig.USER_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
          Provider<RuntimesApi> serviceRuntimesApiProvider,
      Provider<ProxyApi> proxyApiProvider,
      Provider<ServiceInfoApi> serviceInfoApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      NotebooksRetryHandler notebooksRetryHandler,
      LeonardoMapper leonardoMapper,
      LeonardoRetryHandler leonardoRetryHandler,
      WorkspaceDao workspaceDao) {
    this.runtimesApiProvider = runtimesApiProvider;
    this.serviceRuntimesApiProvider = serviceRuntimesApiProvider;
    this.proxyApiProvider = proxyApiProvider;
    this.serviceInfoApiProvider = serviceInfoApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.notebooksRetryHandler = notebooksRetryHandler;
    this.leonardoMapper = leonardoMapper;
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.workspaceDao = workspaceDao;
  }

  private LeonardoCreateRuntimeRequest buildCreateRuntimeRequest(
      String userEmail, Runtime runtime, Map<String, String> customEnvironmentVariables) {
    WorkbenchConfig config = workbenchConfigProvider.get();
    String assetsBaseUrl = config.server.apiBaseUrl + "/static";

    Map<String, String> nbExtensions = new HashMap<>();
    nbExtensions.put("aou-snippets-menu", assetsBaseUrl + "/aou-snippets-menu.js");
    nbExtensions.put("aou-download-extension", assetsBaseUrl + "/aou-download-policy-extension.js");
    nbExtensions.put(
        "aou-activity-checker-extension", assetsBaseUrl + "/activity-checker-extension.js");
    nbExtensions.put(
        "aou-upload-policy-extension", assetsBaseUrl + "/aou-upload-policy-extension.js");

    Map<String, String> runtimeLabels = new HashMap<>();
    runtimeLabels.put(LeonardoMapper.RUNTIME_LABEL_AOU, "true");
    runtimeLabels.put(LeonardoMapper.RUNTIME_LABEL_CREATED_BY, userEmail);

    runtimeLabels.putAll(buildRuntimeConfigurationLabels(runtime.getConfigurationType()));

    LeonardoCreateRuntimeRequest request =
        new LeonardoCreateRuntimeRequest()
            .labels(runtimeLabels)
            .defaultClientId(config.server.oauthClientId)
            // Note: Filenames must be kept in sync with files in api/src/main/webapp/static.
            .jupyterUserScriptUri(assetsBaseUrl + "/initialize_notebook_runtime.sh")
            .jupyterStartUserScriptUri(assetsBaseUrl + "/start_notebook_runtime.sh")
            .userJupyterExtensionConfig(
                new LeonardoUserJupyterExtensionConfig().nbExtensions(nbExtensions))
            // Matches Terra UI's scopes, see RW-3531 for rationale.
            .addScopesItem("https://www.googleapis.com/auth/cloud-platform")
            .addScopesItem("https://www.googleapis.com/auth/userinfo.email")
            .addScopesItem("https://www.googleapis.com/auth/userinfo.profile")
            .toolDockerImage(workbenchConfigProvider.get().firecloud.jupyterDockerImage)
            // Note: DockerHub must be used over GCR here, since VPC-SC restricts
            // pulling external images via GCR (since it counts as GCS traffic).
            .welderRegistry(WelderRegistryEnum.DOCKERHUB)
            .customEnvironmentVariables(customEnvironmentVariables);

    request.setRuntimeConfig(buildRuntimeConfig(runtime));

    return request;
  }

  private Object buildRuntimeConfig(Runtime runtime) {
    if (runtime.getGceConfig() != null) {
      return leonardoMapper.toLeonardoGceConfig(runtime.getGceConfig());
    } else {
      return leonardoMapper.toLeonardoMachineConfig(runtime.getDataprocConfig());
    }
  }

  @Override
  public void createRuntime(
      Runtime runtime, String workspaceNamespace, String workspaceFirecloudName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceFirecloudName);
    DbCdrVersion cdrVersion = workspace.getCdrVersion();
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    // i.e. is NEW or MIGRATED
    if (!workspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
      customEnvironmentVariables.put(
          WORKSPACE_CDR_ENV_KEY,
          cdrVersion.getBigqueryProject()
              + "."
              + cdrVersion.getBigqueryDataset());
    }

    if (cdrVersion.getHasMergedWgsData()) {
      customEnvironmentVariables.put(
          MERGED_WGS_BUCKET_KEY,
          cdrVersion.getAccessTier().getDatasetsBucket()
            + cdrVersion.getCdrVersionId()
            + "/wgs/vcf/merged/"
      );
    }

    if (cdrVersion.getHasSingleSampleArrayData()) {
      customEnvironmentVariables.put(
          SINGLE_SAMPLE_ARRAY_BUCKET_KEY,
          cdrVersion.getAccessTier().getDatasetsBucket()
            + cdrVersion.getCdrVersionId()
            + "/microarray/vcf/single_sample/"
      );
    }

    // See RW-6079
    customEnvironmentVariables.put(JUPYTER_DEBUG_LOGGING_ENV_KEY, "true");

    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.createRuntime(
              runtime.getGoogleProject(),
              runtime.getRuntimeName(),
              buildCreateRuntimeRequest(user.getUsername(), runtime, customEnvironmentVariables));
          return null;
        });
  }

  @Override
  public void updateRuntime(Runtime runtime) {
    Map<String, String> runtimeLabels =
        buildRuntimeConfigurationLabels(runtime.getConfigurationType());

    leonardoRetryHandler.run(
        (context) -> {
          runtimesApiProvider
              .get()
              .updateRuntime(
                  runtime.getGoogleProject(),
                  runtime.getRuntimeName(),
                  new LeonardoUpdateRuntimeRequest()
                      .allowStop(true)
                      .runtimeConfig(buildRuntimeConfig(runtime))
                      .labelsToUpsert(runtimeLabels));
          return null;
        });
  }

  private Map<String, String> buildRuntimeConfigurationLabels(
      RuntimeConfigurationType runtimeConfigurationType) {
    if (runtimeConfigurationType != null) {
      return Collections.singletonMap(
          LeonardoMapper.RUNTIME_LABEL_AOU_CONFIG,
          LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP.get(
              runtimeConfigurationType));
    } else {
      return new HashMap<>();
    }
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesByProjectAsService(String googleProject) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    return leonardoRetryHandler.run(
        (context) -> runtimesApi.listRuntimesByProject(googleProject, null, false));
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesByProject(
      String googleProject, boolean includeDeleted) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    return leonardoRetryHandler.run(
        (context) -> runtimesApi.listRuntimesByProject(googleProject, null, includeDeleted));
  }

  @Override
  public void deleteRuntime(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, /* deleteDisk */ false);
          return null;
        });
  }

  @Override
  public LeonardoGetRuntimeResponse getRuntime(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    try {
      return leonardoRetryHandler.runAndThrowChecked(
          (context) -> runtimesApi.getRuntime(googleProject, runtimeName));
    } catch (ApiException e) {
      throw ExceptionUtils.convertLeonardoException(e);
    }
  }

  @Override
  public void deleteRuntimeAsService(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, /* deleteDisk */ false);
          return null;
        });
  }

  @Override
  public void localize(String googleProject, String runtimeName, Map<String, String> fileList) {
    Localize welderReq =
        new Localize()
            .entries(
                fileList.entrySet().stream()
                    .map(
                        e ->
                            new LocalizationEntry()
                                .sourceUri(e.getValue())
                                .localDestinationPath(e.getKey()))
                    .collect(Collectors.toList()));
    ProxyApi proxyApi = proxyApiProvider.get();
    notebooksRetryHandler.run(
        (context) -> {
          proxyApi.welderLocalize(googleProject, runtimeName, welderReq);
          return null;
        });
  }

  @Override
  public StorageLink createStorageLink(
      String googleProject, String runtime, StorageLink storageLink) {
    ProxyApi proxyApi = proxyApiProvider.get();
    return notebooksRetryHandler.run(
        (context) -> proxyApi.welderCreateStorageLink(googleProject, runtime, storageLink));
  }

  @Override
  public boolean getLeonardoStatus() {
    try {
      serviceInfoApiProvider.get().getSystemStatus();
    } catch (org.pmiops.workbench.leonardo.ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }
}
