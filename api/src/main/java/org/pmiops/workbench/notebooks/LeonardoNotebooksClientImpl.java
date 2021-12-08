package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateDiskRequest;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoUserJupyterExtensionConfig;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
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

  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_BUCKET_KEY = "WORKSPACE_BUCKET";
  private static final String JUPYTER_DEBUG_LOGGING_ENV_KEY = "JUPYTER_DEBUG_LOGGING";
  private static final String ALL_SAMPLES_WGS_KEY = "ALL_SAMPLES_WGS_BUCKET";
  private static final String SINGLE_SAMPLE_ARRAY_BUCKET_KEY = "SINGLE_SAMPLE_ARRAY_BUCKET";

  // Keep in sync with
  // https://github.com/DataBiosphere/leonardo/blob/develop/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/runtimeModels.scala#L162
  private static Set<LeonardoRuntimeStatus> STOPPABLE_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING);

  private static final Logger log = Logger.getLogger(LeonardoNotebooksClientImpl.class.getName());

  private final LeonardoApiClientFactory leonardoApiClientFactory;
  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;
  private final Provider<ProxyApi> proxyApiProvider;
  private final Provider<ServiceInfoApi> serviceInfoApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<DisksApi> diskApiProvider;
  private final FireCloudService fireCloudService;
  private final NotebooksRetryHandler notebooksRetryHandler;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoRetryHandler leonardoRetryHandler;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public LeonardoNotebooksClientImpl(
      LeonardoApiClientFactory leonardoApiClientFactory,
      @Qualifier(NotebooksConfig.USER_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
          Provider<RuntimesApi> serviceRuntimesApiProvider,
      Provider<ProxyApi> proxyApiProvider,
      Provider<ServiceInfoApi> serviceInfoApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      @Qualifier(NotebooksConfig.USER_DISKS_API) Provider<DisksApi> diskApiProvider,
      FireCloudService fireCloudService,
      NotebooksRetryHandler notebooksRetryHandler,
      LeonardoMapper leonardoMapper,
      LeonardoRetryHandler leonardoRetryHandler,
      WorkspaceDao workspaceDao) {
    this.leonardoApiClientFactory = leonardoApiClientFactory;
    this.runtimesApiProvider = runtimesApiProvider;
    this.serviceRuntimesApiProvider = serviceRuntimesApiProvider;
    this.proxyApiProvider = proxyApiProvider;
    this.serviceInfoApiProvider = serviceInfoApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.diskApiProvider = diskApiProvider;
    this.fireCloudService = fireCloudService;
    this.notebooksRetryHandler = notebooksRetryHandler;
    this.leonardoMapper = leonardoMapper;
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.workspaceDao = workspaceDao;
  }

  private LeonardoCreateRuntimeRequest buildCreateRuntimeRequest(
      String userEmail, Runtime runtime, Map<String, String> customEnvironmentVariables) {
    WorkbenchConfig config = workbenchConfigProvider.get();
    String assetsBaseUrl = config.server.apiBaseUrl + "/static";

    Map<String, String> nbExtensions =
        new ImmutableMap.Builder<String, String>()
            .put("aou-snippets-menu", assetsBaseUrl + "/aou-snippets-menu.js")
            .put("aou-download-extension", assetsBaseUrl + "/aou-download-policy-extension.js")
            .put("aou-activity-checker-extension", assetsBaseUrl + "/activity-checker-extension.js")
            .put(
                "aou-file-tree-policy-extension",
                assetsBaseUrl + "/aou-file-tree-policy-extension.js")
            .build();

    Map<String, String> runtimeLabels =
        new ImmutableMap.Builder<String, String>()
            .put(LeonardoMapper.RUNTIME_LABEL_AOU, "true")
            .put(LeonardoMapper.RUNTIME_LABEL_CREATED_BY, userEmail)
            .putAll(buildRuntimeConfigurationLabels(runtime.getConfigurationType()))
            .build();

    LeonardoCreateRuntimeRequest createRuntimeRequest =
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
            .customEnvironmentVariables(customEnvironmentVariables)
            .autopauseThreshold(runtime.getAutopauseThreshold())
            .runtimeConfig(buildRuntimeConfig(runtime));

    // .autopause is ONLY set if the given .autopauseThreshold value should be respected
    // setting to .autopause to `false` will turn off autopause completely and create
    // runtimes that never autopause.
    if (runtime.getAutopauseThreshold() != null) {
      createRuntimeRequest.autopause(true);
    }

    return createRuntimeRequest;
  }

  private Object buildRuntimeConfig(Runtime runtime) {
    if (runtime.getGceConfig() != null) {
      return leonardoMapper.toLeonardoGceConfig(runtime.getGceConfig());
    } else if (runtime.getGceWithPdConfig() != null) {
      return leonardoMapper.toLeonardoGceWithPdConfig(runtime.getGceWithPdConfig());
    } else {
      LeonardoMachineConfig machineConfig =
          leonardoMapper.toLeonardoMachineConfig(runtime.getDataprocConfig());
      if (workbenchConfigProvider.get().featureFlags.enablePrivateDataprocWorker) {
        machineConfig.setWorkerPrivateAccess(true);
      }
      return machineConfig;
    }
  }

  @Override
  public void createRuntime(
      Runtime runtime, String workspaceNamespace, String workspaceFirecloudName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceFirecloudName);
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        fireCloudService
            .getWorkspace(workspace)
            .orElseThrow(() -> new NotFoundException("workspace not found"));

    DbCdrVersion cdrVersion = workspace.getCdrVersion();
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    customEnvironmentVariables.put(WORKSPACE_NAMESPACE_KEY, workspaceNamespace);

    // This variable is already made available by Leonardo, but it's only exported in certain
    // notebooks contexts; this ensures it is always exported. See RW-7096.
    customEnvironmentVariables.put(
        WORKSPACE_BUCKET_KEY, "gs://" + fcWorkspaceResponse.getWorkspace().getBucketName());

    // In Terra V2 workspaces, all compute users have the bigquery.readSessionUser role per CA-1179.
    // In all workspaces, OWNERs have storage read session permission via the project viewer role.
    // If this variable is exported (with any value), codegen will use the BQ storage API, which is
    // ~200x faster for loading large dataframes from Bigquery.
    // After CA-952 is complete, this should always be exported.
    if (WorkspaceAccessLevel.OWNER.toString().equals(fcWorkspaceResponse.getAccessLevel())
        || workspace.isTerraV2Workspace()) {
      customEnvironmentVariables.put(BIGQUERY_STORAGE_API_ENABLED_ENV_KEY, "true");
    }

    // i.e. is NEW or MIGRATED
    if (!workspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
      customEnvironmentVariables.put(
          WORKSPACE_CDR_ENV_KEY,
          cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset());
    }

    if (cdrVersion.getAllSamplesWgsDataBucket() != null) {
      customEnvironmentVariables.put(
          ALL_SAMPLES_WGS_KEY,
          cdrVersion.getAccessTier().getDatasetsBucket().replaceFirst("/$", "")
              + "/"
              + cdrVersion.getAllSamplesWgsDataBucket().replaceFirst("^/", ""));
    }

    if (cdrVersion.getSingleSampleArrayDataBucket() != null) {
      customEnvironmentVariables.put(
          SINGLE_SAMPLE_ARRAY_BUCKET_KEY,
          cdrVersion.getAccessTier().getDatasetsBucket().replaceFirst("/$", "")
              + "/"
              + cdrVersion.getSingleSampleArrayDataBucket().replaceFirst("^/", ""));
    }

    // See RW-6079
    customEnvironmentVariables.put(JUPYTER_DEBUG_LOGGING_ENV_KEY, "true");

    // See RW-7107
    customEnvironmentVariables.put("PYSPARK_PYTHON", "/usr/local/bin/python3");

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
                      .autopause(runtime.getAutopauseThreshold() != null)
                      .autopauseThreshold(runtime.getAutopauseThreshold())
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
  public void deleteRuntime(String googleProject, String runtimeName, Boolean deleteDisk) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, deleteDisk);
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
  public int stopAllUserRuntimesAsService(String userEmail) throws WorkbenchException {
    RuntimesApi runtimesApiAsService = serviceRuntimesApiProvider.get();
    List<LeonardoListRuntimeResponse> runtimes =
        leonardoRetryHandler.run(
            (context) ->
                runtimesApiAsService.listRuntimes(
                    LeonardoMapper.RUNTIME_LABEL_CREATED_BY + "=" + userEmail, false));

    // Only the runtime creator has start/stop permissions, therefore we impersonate here.
    // If/when IA-2996 is resolved, switch this back to the service.
    RuntimesApi runtimesApiAsImpersonatedUser = new RuntimesApi();
    try {
      runtimesApiAsImpersonatedUser.setApiClient(
          leonardoApiClientFactory.newImpersonatedApiClient(userEmail));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    List<Boolean> results =
        runtimes.stream()
            .filter(r -> STOPPABLE_RUNTIME_STATUSES.contains(r.getStatus()))
            .filter(
                r -> {
                  if (!userEmail.equals(r.getAuditInfo().getCreator())) {
                    log.severe(
                        String.format(
                            "listRuntime query by label returned a runtime not created by the expected user: '%s/%s' has creator '%s', expected '%s'",
                            r.getGoogleProject(),
                            r.getRuntimeName(),
                            r.getAuditInfo().getCreator(),
                            userEmail));
                    return false;
                  }
                  return true;
                })
            .parallel()
            .map(
                r -> {
                  try {
                    leonardoRetryHandler.runAndThrowChecked(
                        (context) -> {
                          runtimesApiAsImpersonatedUser.stopRuntime(
                              r.getGoogleProject(), r.getRuntimeName());
                          return null;
                        });
                  } catch (ApiException e) {
                    log.log(
                        Level.WARNING,
                        String.format(
                            "failed to stop runtime '%s/%s'",
                            r.getGoogleProject(), r.getRuntimeName()),
                        e);
                    return false;
                  }
                  return true;
                })
            .collect(Collectors.toList());
    if (results.contains(false)) {
      throw new ServerErrorException("failed to stop all user runtimes, see logs for details");
    }
    return results.size();
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
  public LeonardoGetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = diskApiProvider.get();
    try {
      return leonardoRetryHandler.runAndThrowChecked(
          (context) -> disksApi.getDisk(googleProject, diskName));
    } catch (ApiException e) {
      throw ExceptionUtils.convertLeonardoException(e);
    }
  }

  @Override
  public void deletePersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = diskApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.deleteDisk(googleProject, diskName);
          return null;
        });
  }

  @Override
  public void updatePersistentDisk(String googleProject, String diskName, Integer diskSize)
      throws WorkbenchException {
    DisksApi disksApi = diskApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.updateDisk(
              googleProject, diskName, new LeonardoUpdateDiskRequest().size(diskSize));
          return null;
        });
  }

  @Override
  public List<LeonardoListPersistentDiskResponse> listPersistentDiskByProject(
      String googleProject, boolean includeDeleted) {
    DisksApi disksApi = diskApiProvider.get();
    return leonardoRetryHandler.run(
        (context) -> disksApi.listDisksByProject(googleProject, null, includeDeleted));
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
