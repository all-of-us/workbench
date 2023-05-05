package org.pmiops.workbench.api;

import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME_TRUE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.upsertLeonardoLabel;

import com.google.common.base.Strings;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoApiClient;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoMapper;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.leonardo.model.LeonardoClusterError;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;
import org.pmiops.workbench.model.UpdateRuntimeRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuntimeController implements RuntimeApiDelegate {

  // This file is used by the All of Us libraries to access workspace/CDR metadata.
  private static final String AOU_CONFIG_FILENAME = ".all_of_us_config.json";
  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_ID_KEY = "WORKSPACE_ID";
  private static final String API_HOST_KEY = "API_HOST";
  private static final String BUCKET_NAME_KEY = "BUCKET_NAME";
  private static final String CDR_VERSION_CLOUD_PROJECT = "CDR_VERSION_CLOUD_PROJECT";
  private static final String CDR_VERSION_BIGQUERY_DATASET = "CDR_VERSION_BIGQUERY_DATASET";
  // The billing project to use for the analysis.
  private static final String BILLING_CLOUD_PROJECT = "BILLING_CLOUD_PROJECT";
  private static final String DATA_URI_PREFIX = "data:application/json;base64,";
  private static final String DELOC_PATTERN = "\\.ipynb$";

  private static final Logger log = Logger.getLogger(RuntimeController.class.getName());

  private final LeonardoApiClient leonardoNotebooksClient;
  private final NewLeonardoApiClient newLeonardoClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final LeonardoMapper leonardoMapper;
  private final NewLeonardoMapper newLeonardoMapper;
  private final LeonardoApiHelper leonardoApiHelper;

  @Autowired
  RuntimeController(
      LeonardoApiClient leonardoNotebooksClient,
      NewLeonardoApiClient newLeonardoClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserRecentResourceService userRecentResourceService,
      LeonardoMapper leonardoMapper,
      NewLeonardoMapper newLeonardoMapper,
      LeonardoApiHelper leonardoApiHelper) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.newLeonardoClient = newLeonardoClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.leonardoMapper = leonardoMapper;
    this.newLeonardoMapper = newLeonardoMapper;
    this.leonardoApiHelper = leonardoApiHelper;
  }

  @Override
  public ResponseEntity<Runtime> getRuntime(String workspaceNamespace) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    String googleProject = dbWorkspace.getGoogleProject();
    try {
      LeonardoGetRuntimeResponse leoRuntimeResponse =
          leonardoNotebooksClient.getRuntime(googleProject, user.getRuntimeName());
      if (LeonardoRuntimeStatus.ERROR.equals(leoRuntimeResponse.getStatus())) {
        log.warning(
            String.format(
                "Observed Leonardo runtime with unexpected error status:\n%s",
                formatRuntimeErrors(leoRuntimeResponse.getErrors())));
      }
      return ResponseEntity.ok(leonardoMapper.toApiRuntime(leoRuntimeResponse));
    } catch (NotFoundException e) {
      return ResponseEntity.ok(getOverrideFromListRuntimes(googleProject));
    }
  }

  private String formatRuntimeErrors(@Nullable List<LeonardoClusterError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "no error messages";
    }
    return errors.stream()
        .map(err -> String.format("error %d: %s", err.getErrorCode(), err.getErrorMessage()))
        .collect(Collectors.joining("\n"));
  }

  private Runtime getOverrideFromListRuntimes(String googleProject) {
    Optional<LeonardoListRuntimeResponse> mostRecentRuntimeMaybe =
        leonardoNotebooksClient.listRuntimesByProject(googleProject, true).stream()
            .min(
                (a, b) -> {
                  String aCreatedDate, bCreatedDate;
                  if (a.getAuditInfo() == null || a.getAuditInfo().getCreatedDate() == null) {
                    aCreatedDate = "";
                  } else {
                    aCreatedDate = a.getAuditInfo().getCreatedDate();
                  }

                  if (b.getAuditInfo() == null || b.getAuditInfo().getCreatedDate() == null) {
                    bCreatedDate = "";
                  } else {
                    bCreatedDate = b.getAuditInfo().getCreatedDate();
                  }

                  return bCreatedDate.compareTo(aCreatedDate);
                });

    LeonardoListRuntimeResponse mostRecentRuntime =
        mostRecentRuntimeMaybe.orElseThrow(NotFoundException::new);

    @SuppressWarnings("unchecked")
    Map<String, String> runtimeLabels = (Map<String, String>) mostRecentRuntime.getLabels();

    if (runtimeLabels != null
        && LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
            .values()
            .contains(runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG))) {
      try {
        Runtime runtime = leonardoMapper.toApiRuntime(mostRecentRuntime);
        if (!RuntimeStatus.DELETED.equals(runtime.getStatus())) {
          log.warning(
              "Runtimes returned from ListRuntimes should be DELETED but found "
                  + runtime.getStatus());
        }
        return runtime.status(RuntimeStatus.DELETED);
      } catch (RuntimeException e) {
        log.warning(
            "RuntimeException during LeonardoListRuntimeResponse -> Runtime mapping "
                + e.toString());
      }
    }

    throw new NotFoundException();
  }

  @Override
  public ResponseEntity<EmptyResponse> createRuntime(String workspaceNamespace, Runtime runtime) {
    if (runtime == null) {
      runtime = new Runtime();
    }

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    GceWithPdConfig gceWithPdConfig = runtime.getGceWithPdConfig();
    if (gceWithPdConfig != null) {
      PersistentDiskRequest persistentDiskRequest = gceWithPdConfig.getPersistentDisk();
      if (persistentDiskRequest != null && Strings.isNullOrEmpty(persistentDiskRequest.getName())) {
        // If persistentDiskRequest.getName() is empty, UI wants API to create a new disk.
        // Check with Leo again see if user have READY disk, if so, block this request or logging
        List<Disk> diskList =
            PersistentDiskUtils.findTheMostRecentActiveDisks(
                newLeonardoClient
                    .listPersistentDiskByProjectCreatedByCreator(
                        dbWorkspace.getGoogleProject(), false)
                    .stream()
                    .map(newLeonardoMapper::toApiListDisksResponse)
                    .collect(Collectors.toList()));
        List<Disk> runtimeDisks =
            diskList.stream().filter(Disk::getIsGceRuntime).collect(Collectors.toList());
        if (!runtimeDisks.isEmpty()) {
          // Find active disks for runtime VM. Block user from creating new disk.
          throw new BadRequestException(
              String.format(
                  "Can not create new runtime with new PD if user has active runtime PD. Existing disks: %s",
                  PersistentDiskUtils.prettyPrintDiskNames(runtimeDisks)));
        }
        persistentDiskRequest.name(userProvider.get().generatePDName());
      }
      persistentDiskRequest.labels(
          upsertLeonardoLabel(
              persistentDiskRequest.getLabels(),
              LEONARDO_LABEL_IS_RUNTIME,
              LEONARDO_LABEL_IS_RUNTIME_TRUE));
    }
    long configCount =
        Stream.of(runtime.getGceConfig(), runtime.getDataprocConfig(), runtime.getGceWithPdConfig())
            .filter(c -> c != null)
            .count();
    if (configCount != 1) {
      throw new BadRequestException(
          "Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
    }

    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    leonardoNotebooksClient.createRuntime(
        runtime.googleProject(dbWorkspace.getGoogleProject()).runtimeName(user.getRuntimeName()),
        workspaceNamespace,
        firecloudWorkspaceName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateRuntime(
      String workspaceNamespace, UpdateRuntimeRequest runtimeRequest) {
    if (runtimeRequest == null || runtimeRequest.getRuntime() == null) {
      throw new BadRequestException("Runtime cannot be empty for an update request");
    }

    if (!(runtimeRequest.getRuntime().getGceConfig() != null
        ^ runtimeRequest.getRuntime().getGceWithPdConfig() != null
        ^ runtimeRequest.getRuntime().getDataprocConfig() != null)) {
      throw new BadRequestException(
          "Exactly one of GceConfig, GceWithPdConfig, or DataprocConfig must be provided");
    }

    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    leonardoNotebooksClient.updateRuntime(
        runtimeRequest
            .getRuntime()
            .googleProject(dbWorkspace.getGoogleProject())
            .runtimeName(user.getRuntimeName()));

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRuntime(
      String workspaceNamespace, Boolean deleteDisk) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    leonardoNotebooksClient.deleteRuntime(
        dbWorkspace.getGoogleProject(), user.getRuntimeName(), Boolean.TRUE.equals(deleteDisk));
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<RuntimeLocalizeResponse> localize(
      String workspaceNamespace, RuntimeLocalizeRequest body) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    final RawlsWorkspaceDetails firecloudWorkspace;
    try {
      firecloudWorkspace =
          fireCloudService
              .getWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
              .getWorkspace();
    } catch (NotFoundException e) {
      throw new NotFoundException(
          String.format(
              "workspace %s/%s not found or not accessible",
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName()));
    }
    DbCdrVersion cdrVersion = dbWorkspace.getCdrVersion();

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String gcsNotebooksDir = "gs://" + firecloudWorkspace.getBucketName() + "/notebooks";
    long workspaceId = dbWorkspace.getWorkspaceId();

    body.getNotebookNames()
        .forEach(
            notebookName ->
                userRecentResourceService.updateNotebookEntry(
                    workspaceId,
                    userProvider.get().getUserId(),
                    gcsNotebooksDir + "/" + notebookName));

    String workspacePath = dbWorkspace.getFirecloudName();
    String googleProjectId = dbWorkspace.getGoogleProject();
    String editDir = "workspaces/" + workspacePath;
    String playgroundDir = "workspaces_playground/" + workspacePath;
    String targetDir = body.getPlaygroundMode() ? playgroundDir : editDir;

    leonardoNotebooksClient.createStorageLink(
        googleProjectId,
        user.getRuntimeName(),
        new StorageLink()
            .cloudStorageDirectory(gcsNotebooksDir)
            .localBaseDirectory(editDir)
            .localSafeModeBaseDirectory(playgroundDir)
            .pattern(DELOC_PATTERN));

    // Always localize config files; usually a no-op after the first call.
    Map<String, String> localizeMap = new HashMap<>();

    // The Welder extension offers direct links to/from playground mode; write the AoU config file
    // to both locations so notebooks will work in either directory.
    String aouConfigUri = aouConfigDataUri(firecloudWorkspace, cdrVersion, workspaceNamespace);
    localizeMap.put(editDir + "/" + AOU_CONFIG_FILENAME, aouConfigUri);
    localizeMap.put(playgroundDir + "/" + AOU_CONFIG_FILENAME, aouConfigUri);

    // Localize the requested notebooks, if any.
    if (body.getNotebookNames() != null) {
      localizeMap.putAll(
          body.getNotebookNames().stream()
              .collect(
                  Collectors.toMap(
                      name -> targetDir + "/" + name, name -> gcsNotebooksDir + "/" + name)));
    }
    log.info(localizeMap.toString());
    leonardoNotebooksClient.localize(
        googleProjectId, userProvider.get().getRuntimeName(), localizeMap);

    // This is the Jupyer-server-root-relative path, the style used by the Jupyter REST API.
    return ResponseEntity.ok(new RuntimeLocalizeResponse().runtimeLocalDirectory(targetDir));
  }

  private String jsonToDataUri(JSONObject json) {
    return DATA_URI_PREFIX + Base64.getUrlEncoder().encodeToString(json.toString().getBytes());
  }

  private String aouConfigDataUri(
      RawlsWorkspaceDetails fcWorkspace, DbCdrVersion cdrVersion, String cdrBillingCloudProject) {
    JSONObject config = new JSONObject();

    String host = null;
    try {
      host = new URL(workbenchConfigProvider.get().server.apiBaseUrl).getHost();
    } catch (MalformedURLException e) {
      log.log(Level.SEVERE, "bad apiBaseUrl config value; failing", e);
      throw new ServerErrorException("Failed to generate AoU notebook config");
    }
    config.put(WORKSPACE_NAMESPACE_KEY, fcWorkspace.getNamespace());
    config.put(WORKSPACE_ID_KEY, fcWorkspace.getName());
    config.put(BUCKET_NAME_KEY, fcWorkspace.getBucketName());
    config.put(API_HOST_KEY, host);
    config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersion.getBigqueryProject());
    config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersion.getBigqueryDataset());
    config.put(BILLING_CLOUD_PROJECT, cdrBillingCloudProject);
    return jsonToDataUri(config);
  }
}
