package org.pmiops.workbench.leonardo;

import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.GOOGLE_PROJECT_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.OWNER_EMAIL_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils.WORKSPACE_NAME_ENV_KEY;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.upsertLeonardoLabel;

import com.google.common.base.Strings;
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
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.leonardo.model.LeonardoAppStatus;
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
  private static Set<LeonardoRuntimeStatus> STOPPABLE_RUNTIME_STATUSES =
      ImmutableSet.of(
          LeonardoRuntimeStatus.RUNNING,
          LeonardoRuntimeStatus.STARTING,
          LeonardoRuntimeStatus.UPDATING);

  private static Set<LeonardoAppStatus> STOPPABLE_APP_STATUSES =
      ImmutableSet.of(
          LeonardoAppStatus.RUNNING, LeonardoAppStatus.PROVISIONING, LeonardoAppStatus.STARTING);

  private static final Logger log = Logger.getLogger(LeonardoApiClientImpl.class.getName());

  private final LeonardoApiClientFactory leonardoApiClientFactory;
  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<RuntimesApi> serviceRuntimesApiProvider;
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
      @Qualifier(LeonardoConfig.USER_DISKS_API) Provider<DisksApi> disksApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_DISKS_API) Provider<DisksApi> serviceDisksApiProvider,
      @Qualifier(LeonardoConfig.USER_APPS_API) Provider<AppsApi> appsApiProvider,
      @Qualifier(LeonardoConfig.SERVICE_APPS_API) Provider<AppsApi> serviceAppsApiProvider,
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
    this.disksApiProvider = disksApiProvider;
    this.serviceDisksApiProvider = serviceDisksApiProvider;
    this.appsApiProvider = appsApiProvider;
    this.serviceAppsApiProvider = serviceAppsApiProvider;
    this.fireCloudService = fireCloudService;
    this.notebooksRetryHandler = notebooksRetryHandler;
    this.leonardoMapper = leonardoMapper;
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.workspaceDao = workspaceDao;
  }

  private LeonardoCreateRuntimeRequest buildCreateRuntimeRequest(
      String userEmail, Runtime runtime, Map<String, String> customEnvironmentVariables) {
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

  @Override
  public void createRuntime(
      Runtime runtime, String workspaceNamespace, String workspaceFirecloudName) {
    RuntimesApi runtimesApi = runtimesApiProvider.get();

    DbUser user = userProvider.get();
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceFirecloudName);

    Map<String, String> customEnvironmentVariables =
        LeonardoCustomEnvVarUtils.getBaseEnvironmentVariables(
            workspace, fireCloudService, workbenchConfigProvider.get());

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
                    leonardoRetryHandler.runAndThrowChecked(
                        (context) -> {
                          runtimesApiAsImpersonatedUser.stopRuntime(
                              googleProject, r.getRuntimeName());
                          return null;
                        });
                  } catch (ApiException e) {
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
  public LeonardoGetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
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
          disksApi.updateDisk(
              googleProject, diskName, new LeonardoUpdateDiskRequest().size(diskSize));
          return null;
        });
  }

  @Override
  public List<LeonardoListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject) {

    DisksApi disksApi = disksApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject,
                null,
                /* includeDeleted */ false,
                LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS,
                LEONARDO_CREATOR_ROLE));
  }

  @Override
  public List<LeonardoListPersistentDiskResponse> listDisksByProjectAsService(
      String googleProject) {

    DisksApi disksApi = serviceDisksApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject,
                null,
                /*includeDeleted*/ false,
                LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS,
                /* Leonardo Role */ null));
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

    Map<String, String> appCustomEnvVars =
        LeonardoCustomEnvVarUtils.getBaseEnvironmentVariables(
            dbWorkspace, fireCloudService, workbenchConfigProvider.get());
    // Required by Leo to validate one App per user per workspace (namespace with workspace name)
    appCustomEnvVars.put(WORKSPACE_NAME_ENV_KEY, dbWorkspace.getFirecloudName());

    // Used by AoU RW and but not set by Leo for GKE APP.
    appCustomEnvVars.put(GOOGLE_PROJECT_ENV_KEY, dbWorkspace.getGoogleProject());
    appCustomEnvVars.put(OWNER_EMAIL_ENV_KEY, userProvider.get().getUsername());

    leonardoCreateAppRequest
        .appType(leonardoMapper.toLeonardoAppType(appType))
        .kubernetesRuntimeConfig(
            leonardoMapper.toLeonardoKubernetesRuntimeConfig(kubernetesRuntimeConfig))
        .diskConfig(diskRequest)
        .customEnvironmentVariables(appCustomEnvVars)
        .labels(appLabels);

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
    List<LeonardoListAppResponse> listAppResponses =
        leonardoRetryHandler.run(
            (context) ->
                appsApi.listAppByProject(
                    googleProjectId,
                    /* labels= */ null,
                    /* includeDeleted= */ false,
                    /* includeLabels= */ LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS,
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
    } catch (org.pmiops.workbench.leonardo.ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }

  @Override
  public int stopAllUserAppsAsService(String userEmail) {
    AppsApi appsApiAsService = serviceAppsApiProvider.get();
    List<LeonardoListAppResponse> apps =
        leonardoRetryHandler.run(
            (context) ->
                appsApiAsService.listApp(
                    /* labels =*/ LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY + "=" + userEmail,
                    /* includeDeleted = */ false,
                    /* includeLabels = */ LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS,
                    /* role = */ null));

    // Only the app creator has start/stop permissions, therefore we impersonate here.
    // If/when IA-2996 is resolved, switch this back to the service.
    AppsApi appsApiAsImpersonatedUser = new AppsApi();
    try {
      appsApiAsImpersonatedUser.setApiClient(
          leonardoApiClientFactory.newImpersonatedApiClient(userEmail));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }

    List<Boolean> results =
        apps.stream()
            .filter(r -> STOPPABLE_APP_STATUSES.contains(r.getStatus()))
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
                          appsApiAsImpersonatedUser.stopApp(
                              r.getCloudContext().getCloudResource(), r.getAppName());
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
}
