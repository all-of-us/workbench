package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableList;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.leonardo.model.LeonardoClusterError;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
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

  private static final String runtimeCreatedDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ";

  private static final Logger log = Logger.getLogger(RuntimeController.class.getName());

  private final LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final LeonardoMapper leonardoMapper;

  @Autowired
  RuntimeController(
      LeonardoRuntimeAuditor leonardoRuntimeAuditor,
      LeonardoNotebooksClient leonardoNotebooksClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserRecentResourceService userRecentResourceService,
      LeonardoMapper leonardoMapper) {
    this.leonardoRuntimeAuditor = leonardoRuntimeAuditor;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.leonardoMapper = leonardoMapper;
  }

  private Stream<LeonardoListRuntimeResponse> filterByRuntimesInList(
      Stream<LeonardoListRuntimeResponse> clustersToFilter, List<String> runtimeNames) {
    // Null means keep all clusters.
    return clustersToFilter.filter(
        cluster -> runtimeNames == null || runtimeNames.contains(cluster.getRuntimeName()));
  }

  @Override
  @AuthorityRequired(Authority.SECURITY_ADMIN)
  public ResponseEntity<List<ListRuntimeResponse>> deleteRuntimesInProject(
      String billingProjectId, ListRuntimeDeleteRequest req) {
    if (billingProjectId == null) {
      throw new BadRequestException("Must specify billing project");
    }
    List<LeonardoListRuntimeResponse> runtimesToDelete =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(billingProjectId).stream(),
                req.getRuntimesToDelete())
            .collect(Collectors.toList());

    runtimesToDelete.forEach(
        runtime ->
            leonardoNotebooksClient.deleteRuntimeAsService(
                runtime.getGoogleProject(), runtime.getRuntimeName()));
    List<LeonardoListRuntimeResponse> runtimesInProjectAffected =
        filterByRuntimesInList(
                leonardoNotebooksClient.listRuntimesByProjectAsService(billingProjectId).stream(),
                req.getRuntimesToDelete())
            .collect(Collectors.toList());
    // DELETED is an acceptable status from an implementation standpoint, but we will never
    // receive runtimes with that status from Leo. We don't want to because we reuse runtime
    // names and thus could have >1 deleted runtimes with the same name in the project.
    List<LeonardoRuntimeStatus> acceptableStates =
        ImmutableList.of(LeonardoRuntimeStatus.DELETING, LeonardoRuntimeStatus.ERROR);
    runtimesInProjectAffected.stream()
        .filter(runtime -> !acceptableStates.contains(runtime.getStatus()))
        .forEach(
            clusterInBadState ->
                log.log(
                    Level.SEVERE,
                    String.format(
                        "Runtime %s/%s is not in a deleting state",
                        clusterInBadState.getGoogleProject(), clusterInBadState.getRuntimeName())));
    leonardoRuntimeAuditor.fireDeleteRuntimesInProject(
        billingProjectId,
        runtimesToDelete.stream()
            .map(LeonardoListRuntimeResponse::getRuntimeName)
            .collect(Collectors.toList()));
    return ResponseEntity.ok(
        runtimesInProjectAffected.stream()
            .map(leonardoMapper::toApiListRuntimeResponse)
            .collect(Collectors.toList()));
  }

  private DbWorkspace lookupWorkspace(String workspaceNamespace) throws NotFoundException {
    return workspaceDao
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }

  @Override
  public ResponseEntity<Runtime> getRuntime(String workspaceNamespace) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    String googleProject = dbWorkspace.getGoogleProject();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    try {
      LeonardoGetRuntimeResponse leoRuntimeResponse =
          leonardoNotebooksClient.getRuntime(googleProject, userProvider.get().getRuntimeName());
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
            .sorted(
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
                })
            .findFirst();

    if (!mostRecentRuntimeMaybe.isPresent()) {
      throw new NotFoundException();
    }

    LeonardoListRuntimeResponse mostRecentRuntime = mostRecentRuntimeMaybe.get();

    @SuppressWarnings("unchecked")
    Map<String, String> runtimeLabels = (Map<String, String>) mostRecentRuntime.getLabels();

    if (runtimeLabels != null
        && LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
            .values()
            .contains(runtimeLabels.get(LeonardoMapper.RUNTIME_LABEL_AOU_CONFIG))) {
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

    GceWithPdConfig gceWithPdConfig = runtime.getGceWithPdConfig();
    if (gceWithPdConfig != null) {
      PersistentDiskRequest persistentDiskRequest = gceWithPdConfig.getPersistentDisk();
      if (persistentDiskRequest != null && persistentDiskRequest.getName().isEmpty()) {
        persistentDiskRequest.setName(userProvider.get().generatePDName());
      }
    }
    long configCount =
        Stream.of(runtime.getGceConfig(), runtime.getDataprocConfig(), runtime.getGceWithPdConfig())
            .filter(c -> c != null)
            .count();
    if (configCount != 1) {
      throw new BadRequestException(
          "Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
    }

    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    runtime.setGoogleProject(dbWorkspace.getGoogleProject());
    runtime.setRuntimeName(userProvider.get().getRuntimeName());

    leonardoNotebooksClient.createRuntime(runtime, workspaceNamespace, firecloudWorkspaceName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateRuntime(
      String workspaceNamespace, UpdateRuntimeRequest runtimeRequest) {
    if (runtimeRequest == null || runtimeRequest.getRuntime() == null) {
      throw new BadRequestException("Runtime cannot be empty for an update request");
    }

    if (runtimeRequest.getRuntime().getGceConfig() == null
        && runtimeRequest.getRuntime().getDataprocConfig() == null) {
      throw new BadRequestException("Either a GceConfig or DataprocConfig must be provided");
    }

    if (runtimeRequest.getRuntime().getGceConfig() != null
        && runtimeRequest.getRuntime().getDataprocConfig() != null) {
      throw new BadRequestException("Only one of GceConfig or DataprocConfig must be provided");
    }

    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    runtimeRequest.getRuntime().setGoogleProject(dbWorkspace.getGoogleProject());
    runtimeRequest.getRuntime().setRuntimeName(userProvider.get().getRuntimeName());

    leonardoNotebooksClient.updateRuntime(runtimeRequest.getRuntime());

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRuntime(
      String workspaceNamespace, Boolean deleteDisk) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);

    leonardoNotebooksClient.deleteRuntime(
        dbWorkspace.getGoogleProject(),
        userProvider.get().getRuntimeName(),
        Optional.ofNullable(deleteDisk).orElse(false));
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<RuntimeLocalizeResponse> localize(
      String workspaceNamespace, RuntimeLocalizeRequest body) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    final FirecloudWorkspace firecloudWorkspace;
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
        userProvider.get().getRuntimeName(),
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
      FirecloudWorkspace fcWorkspace, DbCdrVersion cdrVersion, String cdrBillingCloudProject) {
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
