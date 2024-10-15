package org.pmiops.workbench.leonardo;

import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.GOOGLE_PROJECT_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.OWNER_EMAIL_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.WORKSPACE_NAME_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_AOU;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.upsertLeonardoLabel;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Provider;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsApi;
import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AppStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListAppResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.UpdateDiskRequest;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.legacy_leonardo_client.api.ResourcesApi;
import org.pmiops.workbench.legacy_leonardo_client.api.RuntimesApi;
import org.pmiops.workbench.legacy_leonardo_client.api.ServiceInfoApi;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCreateRuntimeRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoMachineConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateDataprocConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateGceConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUpdateRuntimeRequest;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoUserJupyterExtensionConfig;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeConfigurationType;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.notebooks.api.ProxyApi;
import org.pmiops.workbench.notebooks.model.LocalizationEntry;
import org.pmiops.workbench.notebooks.model.Localize;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LeonardoApiClientImpl implements LeonardoApiClient {
  // The Leonardo user role who creates Leonardo APP or disks.
  private static final String LEONARDO_CREATOR_ROLE = "creator";

  // Keep in sync with
  // https://github.com/DataBiosphere/leonardo/blob/develop/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/runtimeModels.scala#L162
  private static final Set<LeonardoRuntimeStatus> STOPPABLE_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING);

  // Keep in sync with
  // https://github.com/DataBiosphere/leonardo/blob/807c024d8e8be86b782e519319520ca3b3705a52/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/kubernetesModels.scala#L522C42-L522C42
  private static final Set<AppStatus> DELETABLE_APP_STATUSES =
      Set.of(AppStatus.STATUS_UNSPECIFIED, AppStatus.RUNNING, AppStatus.ERROR);

  private static final Logger log = Logger.getLogger(LeonardoApiClientImpl.class.getName());

  private final LeonardoApiClientFactory leonardoApiClientFactory;
  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;

  private final Provider<ResourcesApi> resourcesApiProvider;
  private final Provider<ProxyApi> proxyApiProvider;
  private final Provider<ServiceInfoApi> serviceInfoApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<DisksApi> disksApiProvider;
  private final Provider<DisksApi> serviceDisksApiProvider;
  private final Provider<AppsApi> appsApiProvider;
  private final Provider<AppsApi> serviceAppsApiProvider;
  private final FireCloudService fireCloudService;
  private final NotebooksRetryHandler notebooksRetryHandler;
  private final LeonardoMapper leonardoMapper;
  private final LegacyLeonardoRetryHandler legacyLeonardoRetryHandler;
  private final LeonardoRetryHandler leonardoRetryHandler;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public LeonardoApiClientImpl(
      LeonardoApiClientFactory leonardoApiClientFactory,
      @Qualifier(LeonardoConfig.USER_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_RUNTIMES_API)
          Provider<RuntimesApi> serviceRuntimesApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_RESOURCE_API) Provider<ResourcesApi> resourcesApiProvider,
      Provider<ProxyApi> proxyApiProvider,
      Provider<ServiceInfoApi> serviceInfoApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      @Qualifier(LeonardoConfig.USER_DISKS_API) Provider<DisksApi> disksApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_DISKS_API) Provider<DisksApi> serviceDisksApiProvider,
      @Qualifier(LeonardoConfig.USER_APPS_API) Provider<AppsApi> appsApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_APPS_API) Provider<AppsApi> serviceAppsApiProvider,
      FireCloudService fireCloudService,
      NotebooksRetryHandler notebooksRetryHandler,
      LeonardoMapper leonardoMapper,
      LegacyLeonardoRetryHandler legacyLeonardoRetryHandler,
      LeonardoRetryHandler leonardoRetryHandler,
      WorkspaceDao workspaceDao) {
    this.leonardoApiClientFactory = leonardoApiClientFactory;
    this.runtimesApiProvider = runtimesApiProvider;
    this.serviceRuntimesApiProvider = serviceRuntimesApiProvider;
    this.resourcesApiProvider = resourcesApiProvider;
    this.proxyApiProvider = proxyApiProvider;
    this.serviceInfoApiProvider = serviceInfoApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.disksApiProvider = disksApiProvider;
    this.serviceDisksApiProvider = serviceDisksApiProvider;
    this.appsApiProvider = appsApiProvider;
    this.serviceAppsApiProvider = serviceAppsApiProvider;
    this.fireCloudService = fireCloudService;
    this.notebooksRetryHandler = notebooksRetryHandler;
    this.leonardoMapper = leonardoMapper;
    this.legacyLeonardoRetryHandler = legacyLeonardoRetryHandler;
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.workspaceDao = workspaceDao;
  }

  private LeonardoCreateRuntimeRequest buildCreateRuntimeRequest(
      String userEmail,
      Runtime runtime,
      Map<String, String> customEnvironmentVariables,
      String workspaceNamespace,
      String workspaceName) {
    WorkbenchConfig config = workbenchConfigProvider.get();
    String assetsBaseUrl = config.server.apiAssetsBaseUrl + "/static";

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
            .put(LEONARDO_LABEL_AOU, "true")
            .put(LEONARDO_LABEL_CREATED_BY, userEmail)
            .put(LEONARDO_LABEL_WORKSPACE_NAMESPACE, workspaceNamespace)
            .put(LEONARDO_LABEL_WORKSPACE_NAME, workspaceName)
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

  private LeonardoUpdateGceConfig buildUpdateGCEConfig(Runtime runtime) {
    return runtime.getGceConfig() != null
        ? leonardoMapper.toUpdateGceConfig(runtime.getGceConfig())
        : leonardoMapper.toUpdateGceConfig(runtime.getGceWithPdConfig());
  }

  private LeonardoUpdateDataprocConfig buildUpdateDataProcConfig(Runtime runtime) {
    return leonardoMapper.toUpdateDataprocConfig(runtime.getDataprocConfig());
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

    RawlsWorkspaceResponse fcWorkspaceResponse =
        fireCloudService
            .getWorkspace(workspace)
            .orElseThrow(() -> new NotFoundException("workspace not found"));
    Map<String, String> customEnvironmentVariables =
        LeonardoCustomEnvVarUtils.getBaseEnvironmentVariables(
            workspace, fcWorkspaceResponse, workbenchConfigProvider.get());

    // See RW-7107
    customEnvironmentVariables.put("PYSPARK_PYTHON", "/usr/local/bin/python3");

    legacyLeonardoRetryHandler.run(
        (context) -> {
          runtimesApi.createRuntime(
              runtime.getGoogleProject(),
              runtime.getRuntimeName(),
              buildCreateRuntimeRequest(
                  user.getUsername(),
                  runtime,
                  customEnvironmentVariables,
                  workspaceNamespace,
                  workspace.getName()));
          return null;
        });
  }

  private boolean isDataProcRuntime(Runtime runtime) {
    return runtime.getDataprocConfig() != null;
  }

  @Override
  public void updateRuntime(Runtime runtime) {
    Map<String, String> runtimeLabels =
        buildRuntimeConfigurationLabels(runtime.getConfigurationType());

    legacyLeonardoRetryHandler.run(
        (context) -> {
          runtimesApiProvider
              .get()
              .updateRuntime(
                  runtime.getGoogleProject(),
                  runtime.getRuntimeName(),
                  new LeonardoUpdateRuntimeRequest()
                      .allowStop(true)
                      .runtimeConfig(
                          isDataProcRuntime(runtime)
                              ? buildUpdateDataProcConfig(runtime)
                              : buildUpdateGCEConfig(runtime))
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
          LEONARDO_LABEL_AOU_CONFIG,
          LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP.get(
              runtimeConfigurationType));
    } else {
      return new HashMap<>();
    }
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesAsService() {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    return legacyLeonardoRetryHandler.run(
        (context) -> runtimesApi.listRuntimes(/* labels */ null, /* includeDeleted */ false));
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesByProjectAsService(String googleProject) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    return legacyLeonardoRetryHandler.run(
        (context) ->
            runtimesApi.listRuntimesByProject(
                googleProject, /* labels */ null, /* includeDeleted */ false));
  }

  @Override
  public List<LeonardoListRuntimeResponse> listRuntimesByProject(
      String googleProject, boolean includeDeleted) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    return legacyLeonardoRetryHandler.run(
        (context) ->
            runtimesApi.listRuntimesByProject(googleProject, /* labels */ null, includeDeleted));
  }

  @Override
  public void deleteRuntime(String googleProject, String runtimeName, Boolean deleteDisk) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    legacyLeonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, deleteDisk);
          return null;
        });
  }

  @Override
  public LeonardoGetRuntimeResponse getRuntime(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();
    try {
      return legacyLeonardoRetryHandler.runAndThrowChecked(
          (context) -> runtimesApi.getRuntime(googleProject, runtimeName));
    } catch (org.pmiops.workbench.legacy_leonardo_client.ApiException e) {
      throw ExceptionUtils.convertLegacyLeonardoException(e);
    }
  }

  @Override
  public LeonardoGetRuntimeResponse getRuntimeAsService(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    return legacyLeonardoRetryHandler.run(
        (context) -> runtimesApi.getRuntime(googleProject, runtimeName));
  }

  @Override
  public void deleteRuntimeAsService(String googleProject, String runtimeName) {
    RuntimesApi runtimesApi = serviceRuntimesApiProvider.get();
    legacyLeonardoRetryHandler.run(
        (context) -> {
          runtimesApi.deleteRuntime(googleProject, runtimeName, /* deleteDisk */ false);
          return null;
        });
  }

  @Override
  public int stopAllUserRuntimesAsService(String userEmail) throws WorkbenchException {
    RuntimesApi runtimesApiAsService = serviceRuntimesApiProvider.get();
    List<LeonardoListRuntimeResponse> runtimes =
        legacyLeonardoRetryHandler.run(
            (context) ->
                runtimesApiAsService.listRuntimes(
                    LEONARDO_LABEL_CREATED_BY + "=" + userEmail, false));

    // Only the runtime creator has start/stop permissions, therefore we impersonate here.
    // If/when IA-2996 is resolved, switch this back to the service.
    RuntimesApi runtimesApiAsImpersonatedUser = new RuntimesApi();
    try {
      runtimesApiAsImpersonatedUser.setApiClient(
          leonardoApiClientFactory.newImpersonatedLegacyApiClient(userEmail));
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
                            leonardoMapper.toGoogleProject(r.getCloudContext()),
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
                  String googleProject = leonardoMapper.toGoogleProject(r.getCloudContext());
                  try {
                    legacyLeonardoRetryHandler.runAndThrowChecked(
                        (context) -> {
                          runtimesApiAsImpersonatedUser.stopRuntime(
                              googleProject, r.getRuntimeName());
                          return null;
                        });
                  } catch (org.pmiops.workbench.legacy_leonardo_client.ApiException e) {
                    log.log(
                        Level.WARNING,
                        String.format(
                            "failed to stop runtime '%s/%s'", googleProject, r.getRuntimeName()),
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
  public void localizeForRuntime(
      String googleProject, String runtimeName, Map<String, String> fileList) {
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
          proxyApi.welderLocalize(welderReq, googleProject, runtimeName);
          return null;
        });
  }

  @Override
  public StorageLink createStorageLinkForApp(
      String googleProject, String appName, StorageLink storageLink) {
    ProxyApi proxyApi = proxyApiProvider.get();
    return notebooksRetryHandler.run(
        (context) -> proxyApi.welderCreateStorageLinkForApp(storageLink, googleProject, appName));
  }

  @Override
  public void localizeForApp(String googleProject, String appName, Map<String, String> fileList) {
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
          proxyApi.welderLocalizeForApp(welderReq, googleProject, appName);
          return null;
        });
  }

  @Override
  public StorageLink createStorageLinkForRuntime(
      String googleProject, String runtime, StorageLink storageLink) {
    ProxyApi proxyApi = proxyApiProvider.get();
    return notebooksRetryHandler.run(
        (context) -> proxyApi.welderCreateStorageLink(storageLink, googleProject, runtime));
  }

  @Override
  public void deletePersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.deleteDisk(googleProject, diskName);
          return null;
        });
  }

  @Override
  public void deletePersistentDiskAsService(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = serviceDisksApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.deleteDisk(googleProject, diskName);
          return null;
        });
  }

  @Override
  public void updatePersistentDisk(String googleProject, String diskName, Integer diskSize)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.updateDisk(googleProject, diskName, new UpdateDiskRequest().size(diskSize));
          return null;
        });
  }

  @Override
  public List<ListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject) {

    var disksApi = disksApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject,
                /* labels */ null,
                /* includeDeleted */ false,
                LEONARDO_DISK_LABEL_KEYS,
                LEONARDO_CREATOR_ROLE));
  }

  @Override
  public List<ListPersistentDiskResponse> listDisksAsService() {
    DisksApi disksApi = serviceDisksApiProvider.get();

    // this call can be slow, so let a long timeout
    disksApi
        .getApiClient()
        .setReadTimeout(workbenchConfigProvider.get().firecloud.lenientTimeoutInSeconds * 1000);

    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisks(
                /* labels */ null,
                /* includeDeleted */ false,
                LEONARDO_DISK_LABEL_KEYS,
                /* Leonardo Role */ null));
  }

  @Override
  public List<ListPersistentDiskResponse> listDisksByProjectAsService(String googleProject) {

    DisksApi disksApi = serviceDisksApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject,
                /* labels */ null,
                /* includeDeleted */ false,
                LEONARDO_DISK_LABEL_KEYS,
                /* Leonardo Role */ null));
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace)
      throws WorkbenchException {
    AppsApi appsApi = appsApiProvider.get();

    AppType appType = createAppRequest.getAppType();
    KubernetesRuntimeConfig kubernetesRuntimeConfig = createAppRequest.getKubernetesRuntimeConfig();
    PersistentDiskRequest persistentDiskRequest = createAppRequest.getPersistentDiskRequest();

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest
        leonardoCreateAppRequest =
            new org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest();
    Map<String, String> appLabels =
        new ImmutableMap.Builder<String, String>()
            .put(LEONARDO_LABEL_AOU, "true")
            .put(LEONARDO_LABEL_CREATED_BY, userProvider.get().getUsername())
            .put(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appType))
            .put(LEONARDO_LABEL_WORKSPACE_NAMESPACE, dbWorkspace.getWorkspaceNamespace())
            .put(LEONARDO_LABEL_WORKSPACE_NAME, dbWorkspace.getName())
            .build();

    var pdLabels = persistentDiskRequest.getLabels();
    pdLabels = upsertLeonardoLabel(pdLabels, LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appType));
    pdLabels =
        upsertLeonardoLabel(
            pdLabels, LEONARDO_LABEL_WORKSPACE_NAMESPACE, dbWorkspace.getWorkspaceNamespace());
    pdLabels = upsertLeonardoLabel(pdLabels, LEONARDO_LABEL_WORKSPACE_NAME, dbWorkspace.getName());
    org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest diskRequest =
        leonardoMapper.toLeonardoPersistentDiskRequest(persistentDiskRequest).labels(pdLabels);
    // If no disk name in field name from request, that means creating new disk.
    if (Strings.isNullOrEmpty(diskRequest.getName())) {
      // If persistentDiskRequest.getName() is empty, UI wants API to create a new disk.
      // Check with Leo again see if user have READY disk, if so, block this request or logging

      // Filter out the disks returned by 'listPersistentDiskByProjectCreatedByCreator'
      // that may have a null 'appType', as these disks are associated with Jupyter
      List<Disk> diskList =
          PersistentDiskUtils.findTheMostRecentActiveDisks(
              listPersistentDiskByProjectCreatedByCreator(dbWorkspace.getGoogleProject()).stream()
                  .map(leonardoMapper::toApiListDisksResponse)
                  .filter(disk -> disk.getAppType() != null)
                  .collect(Collectors.toList()));

      List<Disk> appDisks =
          diskList.stream()
              .filter(d -> d.getAppType().equals(createAppRequest.getAppType()))
              .collect(Collectors.toList());
      if (!appDisks.isEmpty()) {
        // Find active disks for APP VM. Block user from creating new disk.
        throw new BadRequestException(
            String.format(
                "Can not create new APP with new PD if user has active APP PD. Existing disks: %s",
                PersistentDiskUtils.prettyPrintDiskNames(appDisks)));
      }
      diskRequest.setName(userProvider.get().generatePDNameForUserApps(appType));
    }

    RawlsWorkspaceResponse fcWorkspaceResponse =
        fireCloudService
            .getWorkspace(dbWorkspace)
            .orElseThrow(() -> new NotFoundException("workspace not found"));

    Map<String, String> appCustomEnvVars =
        LeonardoCustomEnvVarUtils.getBaseEnvironmentVariables(
            dbWorkspace, fcWorkspaceResponse, workbenchConfigProvider.get());
    // Required by Leo to validate one App per user per workspace (namespace with workspace name)
    appCustomEnvVars.put(WORKSPACE_NAME_ENV_KEY, dbWorkspace.getFirecloudName());

    // Used by AoU RW and but not set by Leo for GKE APP.
    appCustomEnvVars.put(GOOGLE_PROJECT_ENV_KEY, dbWorkspace.getGoogleProject());
    appCustomEnvVars.put(OWNER_EMAIL_ENV_KEY, userProvider.get().getUsername());

    leonardoCreateAppRequest
        .appType(leonardoMapper.toLeonardoAppType(appType))
        .allowedChartName(leonardoMapper.toLeonardoAllowedChartName(appType))
        .kubernetesRuntimeConfig(
            leonardoMapper.toLeonardoKubernetesRuntimeConfig(kubernetesRuntimeConfig))
        .diskConfig(diskRequest)
        .customEnvironmentVariables(appCustomEnvVars)
        .workspaceId(dbWorkspace.getFirecloudUuid())
        .labels(appLabels)
        .autodeleteEnabled(createAppRequest.isAutodeleteEnabled())
        .autodeleteThreshold(createAppRequest.getAutodeleteThreshold());

    if (workbenchConfigProvider.get().featureFlags.enableGcsFuseOnGke
        && (appType.equals(AppType.RSTUDIO) || appType.equals(AppType.SAS))) {
      leonardoCreateAppRequest.bucketNameToMount(
          fcWorkspaceResponse.getWorkspace().getBucketName());
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

    GetAppResponse leonardoGetAppResponse =
        leonardoRetryHandler.run((context) -> appsApi.getApp(googleProjectId, appName));
    return leonardoMapper.toApiApp(leonardoGetAppResponse);
  }

  @Override
  public List<UserAppEnvironment> listAppsInProjectCreatedByCreator(String googleProjectId) {
    AppsApi appsApi = appsApiProvider.get();
    return getUserAppEnvironments(googleProjectId, appsApi, LEONARDO_CREATOR_ROLE);
  }

  @Override
  public List<UserAppEnvironment> listAppsInProjectAsService(String googleProjectId) {
    AppsApi appsApi = serviceAppsApiProvider.get();
    return getUserAppEnvironments(googleProjectId, appsApi, null);
  }

  @NotNull
  private List<UserAppEnvironment> getUserAppEnvironments(
      String googleProjectId, AppsApi appsApi, String leonardoAppRole) {
    List<ListAppResponse> listAppResponses =
        leonardoRetryHandler.run(
            (context) ->
                appsApi.listAppByProject(
                    googleProjectId,
                    /* labels= */ null,
                    /* includeDeleted= */ false,
                    /* includeLabels= */ LEONARDO_APP_LABEL_KEYS,
                    leonardoAppRole));

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
    } catch (org.pmiops.workbench.legacy_leonardo_client.ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }

  /** Deletes user apps (exclude Cromwell) and keep user disk. */
  @Override
  public int deleteUserAppsAsService(String userEmail) {
    AppsApi appsApiAsService = serviceAppsApiProvider.get();
    List<ListAppResponse> apps =
        leonardoRetryHandler.run(
            (context) ->
                appsApiAsService.listApp(
                    /* labels= */ LEONARDO_LABEL_CREATED_BY + "=" + userEmail,
                    /* includeDeleted= */ false,
                    /* includeLabels= */ LEONARDO_APP_LABEL_KEYS,
                    /* role= */ null));

    List<Boolean> results =
        apps.stream()
            .filter(r -> DELETABLE_APP_STATUSES.contains(r.getStatus()))
            .filter(
                r ->
                    r.getAppType()
                        != org.broadinstitute.dsde.workbench.client.leonardo.model.AppType
                            .CROMWELL) // Don't delete Cromwell
            .filter(
                r -> {
                  if (!userEmail.equals(r.getAuditInfo().getCreator())) {
                    log.severe(
                        String.format(
                            "listApp query by label returned an app not created by the expected user: '%s/%s' has creator '%s', expected '%s'",
                            r.getCloudContext().getCloudResource(),
                            r.getAppName(),
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
                          appsApiAsService.deleteApp(
                              r.getCloudContext().getCloudResource(), r.getAppName(), false);
                          return null;
                        });
                  } catch (ApiException e) {
                    log.log(
                        Level.WARNING,
                        String.format(
                            "failed to stop runtime '%s/%s'",
                            r.getCloudContext().getCloudResource(), r.getAppName()),
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
  public void deleteAllResources(String googleProject, boolean deleteDisk) {
    legacyLeonardoRetryHandler.run(
        (context) -> {
          try {
            resourcesApiProvider.get().deleteAllResources(googleProject, deleteDisk);
          } catch (org.pmiops.workbench.legacy_leonardo_client.ApiException e) {
            throw ExceptionUtils.convertLegacyLeonardoException(e);
          }
          return null;
        });
  }
}
