package org.pmiops.workbench.leonardo;

import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.upsertLeonardoLabel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.leonardo.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoGetAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListAppResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateDiskRequest;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.leonardo.model.LeonardoUserJupyterExtensionConfig;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoApiClientImpl implements LeonardoApiClient {

  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_BUCKET_KEY = "WORKSPACE_BUCKET";
  private static final String JUPYTER_DEBUG_LOGGING_ENV_KEY = "JUPYTER_DEBUG_LOGGING";
  private static final String LEONARDO_BASE_URL = "LEONARDO_BASE_URL";

  private static final String CDR_STORAGE_PATH_KEY = "CDR_STORAGE_PATH";
  private static final String WGS_VCF_MERGED_STORAGE_PATH_KEY = "WGS_VCF_MERGED_STORAGE_PATH";
  private static final String WGS_HAIL_STORAGE_PATH_KEY = "WGS_HAIL_STORAGE_PATH";

  @VisibleForTesting
  public static final String WGS_CRAM_MANIFEST_PATH_KEY = "WGS_CRAM_MANIFEST_PATH";

  private static final String MICROARRAY_HAIL_STORAGE_PATH_KEY = "MICROARRAY_HAIL_STORAGE_PATH";
  private static final String MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH_KEY =
      "MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH";
  private static final String MICROARRAY_VCF_MANIFEST_PATH_KEY = "MICROARRAY_VCF_MANIFEST_PATH";
  private static final String MICROARRAY_IDAT_MANIFEST_PATH_KEY = "MICROARRAY_IDAT_MANIFEST_PATH";

  // The Leonardo user role who creates Leonardo APP or disks.
  private static final String LEONARDO_CREATOR_ROLE = "creator";

  // Keep in sync with
  // https://github.com/DataBiosphere/leonardo/blob/develop/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/runtimeModels.scala#L162
  private static Set<LeonardoRuntimeStatus> STOPPABLE_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING);

  private static final Logger log = Logger.getLogger(LeonardoApiClientImpl.class.getName());

  private final LeonardoApiClientFactory leonardoApiClientFactory;
  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;
  private final Provider<ProxyApi> proxyApiProvider;
  private final Provider<ServiceInfoApi> serviceInfoApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<DisksApi> diskApiProvider;
  private final Provider<AppsApi> appsApiProvider;
  private final FireCloudService fireCloudService;
  private final NotebooksRetryHandler notebooksRetryHandler;
  private final LeonardoMapper leonardoMapper;
  private final LeonardoRetryHandler leonardoRetryHandler;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public LeonardoApiClientImpl(
      LeonardoApiClientFactory leonardoApiClientFactory,
      @Qualifier(LeonardoConfig.USER_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_RUNTIMES_API)
          Provider<RuntimesApi> serviceRuntimesApiProvider,
      Provider<ProxyApi> proxyApiProvider,
      Provider<ServiceInfoApi> serviceInfoApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      @Qualifier(LeonardoConfig.USER_DISKS_API) Provider<DisksApi> diskApiProvider,
      Provider<AppsApi> appsApiProvider,
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
    this.appsApiProvider = appsApiProvider;
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
            .put(LeonardoLabelHelper.LEONARDO_LABEL_AOU, "true")
            .put(LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY, userEmail)
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
      return leonardoMapper
          .toLeonardoGceConfig(runtime.getGceConfig())
          .zone(workbenchConfigProvider.get().firecloud.gceVmZone);
    } else if (runtime.getGceWithPdConfig() != null) {
      return leonardoMapper
          .toLeonardoGceWithPdConfig(runtime.getGceWithPdConfig())
          .zone(workbenchConfigProvider.get().firecloud.gceVmZone);
    } else {
      LeonardoMachineConfig machineConfig =
          leonardoMapper.toLeonardoMachineConfig(runtime.getDataprocConfig());
      if (workbenchConfigProvider.get().featureFlags.enablePrivateDataprocWorker) {
        machineConfig.setWorkerPrivateAccess(true);
      }
      return machineConfig;
    }
  }

  private String joinStoragePaths(String... paths) {
    final CharMatcher slashMatch = CharMatcher.is('/');
    return Arrays.stream(paths)
        .map(slashMatch::trimLeadingFrom)
        .map(slashMatch::trimTrailingFrom)
        .filter(p -> !p.isEmpty())
        .collect(Collectors.joining("/"));
  }

  private Map<String, String> buildCdrEnvVars(DbCdrVersion cdrVersion) {
    Map<String, String> vars = new HashMap<>();
    vars.put(
        WORKSPACE_CDR_ENV_KEY,
        cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset());

    String datasetsBucket = cdrVersion.getAccessTier().getDatasetsBucket();
    String bucketInfix = cdrVersion.getStorageBasePath();
    if (!Strings.isNullOrEmpty(datasetsBucket) && !Strings.isNullOrEmpty(bucketInfix)) {
      String basePath = joinStoragePaths(datasetsBucket, bucketInfix);
      Map<String, Optional<String>> partialStoragePaths =
          ImmutableMap.<String, Optional<String>>builder()
              .put(CDR_STORAGE_PATH_KEY, Optional.of("/"))
              .put(
                  WGS_VCF_MERGED_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsVcfMergedStoragePath()))
              .put(
                  WGS_HAIL_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsHailStoragePath()))
              .put(
                  WGS_CRAM_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getWgsCramManifestPath()))
              .put(
                  MICROARRAY_HAIL_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayHailStoragePath()))
              .put(
                  MICROARRAY_VCF_SINGLE_SAMPLE_STORAGE_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayVcfSingleSampleStoragePath()))
              .put(
                  MICROARRAY_VCF_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayVcfManifestPath()))
              .put(
                  MICROARRAY_IDAT_MANIFEST_PATH_KEY,
                  Optional.ofNullable(cdrVersion.getMicroarrayIdatManifestPath()))
              .build();
      vars.putAll(
          partialStoragePaths.entrySet().stream()
              .filter(entry -> entry.getValue().filter(p -> !p.isEmpty()).isPresent())
              .collect(
                  Collectors.toMap(
                      Entry::getKey, entry -> joinStoragePaths(basePath, entry.getValue().get()))));
    }

    return vars;
  }

  @Override
  public void createRuntime(
      Runtime runtime, String workspaceNamespace, String workspaceFirecloudName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceFirecloudName);

    Map<String, String> customEnvironmentVariables = getBaseEnvironmentVariables(workspace);

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
          LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG,
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
                    LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY + "=" + userEmail, false));

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
  public List<LeonardoListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject, boolean includeDeleted) {
    DisksApi disksApi = diskApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject, null, includeDeleted, LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS, LEONARDO_CREATOR_ROLE));
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace)
      throws WorkbenchException {
    AppsApi appsApi = appsApiProvider.get();

    AppType appType = createAppRequest.getAppType();
    KubernetesRuntimeConfig kubernetesRuntimeConfig = createAppRequest.getKubernetesRuntimeConfig();
    PersistentDiskRequest persistentDiskRequest = createAppRequest.getPersistentDiskRequest();

    LeonardoCreateAppRequest leonardoCreateAppRequest = new LeonardoCreateAppRequest();
    Map<String, String> appLabels =
        new ImmutableMap.Builder<String, String>()
            .put(LeonardoLabelHelper.LEONARDO_LABEL_AOU, "true")
            .put(LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY, userProvider.get().getUsername())
            .put(LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appType))
            .build();

    LeonardoPersistentDiskRequest diskRequest =
        leonardoMapper
            .toLeonardoPersistentDiskRequest(persistentDiskRequest)
            .labels(
                upsertLeonardoLabel(
                    persistentDiskRequest.getLabels(),
                    LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE,
                    appTypeToLabelValue(appType)));
    // If no disk name in field name from request, that means creating new disk.
    if (Strings.isNullOrEmpty(diskRequest.getName())) {
      diskRequest.setName(userProvider.get().generatePDNameForUserApps(appType));
    }

    leonardoCreateAppRequest
        .appType(leonardoMapper.toLeonardoAppType(appType))
        .kubernetesRuntimeConfig(
            leonardoMapper.toLeonardoKubernetesRuntimeConfig(kubernetesRuntimeConfig))
        .diskConfig(diskRequest)
        .customEnvironmentVariables(getBaseEnvironmentVariables(dbWorkspace))
        .labels(appLabels);

    if (appType.equals(AppType.RSTUDIO)) {
      leonardoCreateAppRequest.descriptorPath(
          workbenchConfigProvider.get().firecloud.userApps.rStudioDescriptorPath);
    }

    leonardoRetryHandler.run(
        (context) -> {
          appsApi.createApp(
              dbWorkspace.getGoogleProject(),
              userProvider.get().generateUserAppName(appType),
              leonardoCreateAppRequest);
          return null;
        });
  }

  @Override
  public UserAppEnvironment getAppByNameByProjectId(String googleProjectId, String appName) {
    AppsApi appsApi = appsApiProvider.get();

    LeonardoGetAppResponse leonardoGetAppResponse =
        leonardoRetryHandler.run((context) -> appsApi.getApp(googleProjectId, appName));
    return leonardoMapper.toApiApp(leonardoGetAppResponse);
  }

  @Override
  public List<UserAppEnvironment> listAppsInProjectCreatedByCreator(String googleProjectId) {
    AppsApi appsApi = appsApiProvider.get();
    List<LeonardoListAppResponse> listAppResponses =
        leonardoRetryHandler.run(
            (context) ->
                appsApi.listAppByProject(
                    googleProjectId,
                    /* labels= */ null,
                    /* includeDeleted= */ false,
                    /* includeLabels= */ LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS, LEONARDO_CREATOR_ROLE));

    return listAppResponses.stream().map(leonardoMapper::toApiApp).collect(Collectors.toList());
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, boolean deleteDisk)
      throws WorkbenchException {
    AppsApi appsApi = appsApiProvider.get();

    leonardoRetryHandler.run(
        (context) -> {
          appsApi.deleteApp(dbWorkspace.getGoogleProject(), appName, deleteDisk);
          return null;
        });
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

  /** The general environment variables that can be used in all Apps. */
  private Map<String, String> getBaseEnvironmentVariables(DbWorkspace workspace) {
    Map<String, String> customEnvironmentVariables = new HashMap<>();
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        fireCloudService
            .getWorkspace(workspace)
            .orElseThrow(() -> new NotFoundException("workspace not found"));
    customEnvironmentVariables.put(WORKSPACE_NAMESPACE_KEY, workspace.getWorkspaceNamespace());
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
    customEnvironmentVariables.put(
        LEONARDO_BASE_URL, workbenchConfigProvider.get().firecloud.leoBaseUrl);
    customEnvironmentVariables.putAll(buildCdrEnvVars(workspace.getCdrVersion()));

    return customEnvironmentVariables;
  }
}
