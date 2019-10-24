package org.pmiops.workbench.api;

import com.google.gson.Gson;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.CdrVersionEntity;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.User.ClusterConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.model.ClusterError;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {

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

  private static final Logger log = Logger.getLogger(ClusterController.class.getName());

  private static final Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster>
      TO_ALL_OF_US_CLUSTER =
          (firecloudCluster) -> {
            Cluster allOfUsCluster = new Cluster();
            allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
            allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject());
            ClusterStatus status = ClusterStatus.UNKNOWN;
            if (firecloudCluster.getStatus() != null) {
              ClusterStatus converted =
                  ClusterStatus.fromValue(firecloudCluster.getStatus().toString());
              if (converted != null) {
                status = converted;
              } else {
                log.warning("unknown Leonardo status: " + firecloudCluster.getStatus());
              }
            }
            allOfUsCluster.setStatus(status);
            allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
            return allOfUsCluster;
          };

  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final Provider<User> userProvider;
  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;
  private final UserRecentResourceService userRecentResourceService;
  private final UserDao userDao;
  private final Clock clock;

  @Autowired
  ClusterController(
      LeonardoNotebooksClient leonardoNotebooksClient,
      Provider<User> userProvider,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      UserRecentResourceService userRecentResourceService,
      UserDao userDao,
      Clock clock) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userService = userService;
    this.userRecentResourceService = userRecentResourceService;
    this.userDao = userDao;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<ClusterListResponse> listClusters(String billingProjectId) {
    if (billingProjectId == null) {
      throw new BadRequestException("Must specify billing project");
    }

    User user = this.userProvider.get();

    String clusterName = clusterNameForUser(user);

    org.pmiops.workbench.notebooks.model.Cluster fcCluster;
    try {
      fcCluster = this.leonardoNotebooksClient.getCluster(billingProjectId, clusterName);
    } catch (NotFoundException e) {
      fcCluster = this.leonardoNotebooksClient.createCluster(billingProjectId, clusterName);
    }

    int retries = Optional.ofNullable(user.getClusterCreateRetries()).orElse(0);
    if (org.pmiops.workbench.notebooks.model.ClusterStatus.ERROR.equals(fcCluster.getStatus())) {
      if (retries <= 2) {
        this.userService.setClusterRetryCount(retries + 1);
        log.warning("Cluster has errored with logs: ");
        if (fcCluster.getErrors() != null) {
          for (ClusterError e : fcCluster.getErrors()) {
            log.warning(e.getErrorMessage());
          }
        }
        log.warning("Retrying cluster creation.");

        this.leonardoNotebooksClient.deleteCluster(billingProjectId, clusterName);
      }
    } else if (org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING.equals(
            fcCluster.getStatus())
        && retries != 0) {
      this.userService.setClusterRetryCount(0);
    }
    ClusterListResponse resp = new ClusterListResponse();
    resp.setDefaultCluster(TO_ALL_OF_US_CLUSTER.apply(fcCluster));
    return ResponseEntity.ok(resp);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCluster(String projectName, String clusterName) {
    this.userService.setClusterRetryCount(0);
    this.leonardoNotebooksClient.deleteCluster(projectName, clusterName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ClusterLocalizeResponse> localize(
      String projectName, String clusterName, ClusterLocalizeRequest body) {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;
    try {
      fcWorkspace =
          fireCloudService
              .getWorkspace(body.getWorkspaceNamespace(), body.getWorkspaceId())
              .getWorkspace();
    } catch (NotFoundException e) {
      throw new NotFoundException(
          String.format(
              "workspace %s/%s not found or not accessible",
              body.getWorkspaceNamespace(), body.getWorkspaceId()));
    }
    CdrVersionEntity cdrVersionEntity =
        workspaceService
            .getRequired(body.getWorkspaceNamespace(), body.getWorkspaceId())
            .getCdrVersionEntity();

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String gcsNotebooksDir = "gs://" + fcWorkspace.getBucketName() + "/notebooks";
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long workspaceId =
        workspaceService
            .getRequired(body.getWorkspaceNamespace(), body.getWorkspaceId())
            .getWorkspaceId();

    body.getNotebookNames()
        .forEach(
            notebook ->
                userRecentResourceService.updateNotebookEntry(
                    workspaceId,
                    userProvider.get().getUserId(),
                    gcsNotebooksDir + "/" + notebook,
                    now));
    String workspacePath = body.getWorkspaceId();
    if (!projectName.equals(body.getWorkspaceNamespace())) {
      workspacePath =
          body.getWorkspaceNamespace()
              + FireCloudService.WORKSPACE_DELIMITER
              + body.getWorkspaceId();
    }

    String editDir = "workspaces/" + workspacePath;
    String playgroundDir = "workspaces_playground/" + workspacePath;
    String targetDir = body.getPlaygroundMode() ? playgroundDir : editDir;

    leonardoNotebooksClient.createStorageLink(
        projectName,
        clusterName,
        new StorageLink()
            .cloudStorageDirectory(gcsNotebooksDir)
            .localBaseDirectory(editDir)
            .localSafeModeBaseDirectory(playgroundDir)
            .pattern(DELOC_PATTERN));

    // Always localize config files; usually a no-op after the first call.
    Map<String, String> localizeMap = new HashMap<>();

    // The Welder extension offers direct links to/from playground mode; write the AoU config file
    // to both locations so notebooks will work in either directory.
    String aouConfigUri = aouConfigDataUri(fcWorkspace, cdrVersionEntity, projectName);
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

    leonardoNotebooksClient.localize(projectName, clusterName, localizeMap);

    // This is the Jupyer-server-root-relative path, the style used by the Jupyter REST API.
    return ResponseEntity.ok(new ClusterLocalizeResponse().clusterLocalDirectory(targetDir));
  }

  @Override
  @AuthorityRequired({Authority.DEVELOPER})
  public ResponseEntity<EmptyResponse> updateClusterConfig(UpdateClusterConfigRequest body) {
    User user = userDao.findUserByEmail(body.getUserEmail());
    if (user == null) {
      throw new NotFoundException("User '" + body.getUserEmail() + "' not found");
    }
    String oldOverride = user.getClusterConfigDefaultRaw();

    final ClusterConfig override = body.getClusterConfig() != null ? new ClusterConfig() : null;
    if (override != null) {
      override.masterDiskSize = body.getClusterConfig().getMasterDiskSize();
      override.machineType = body.getClusterConfig().getMachineType();
    }
    userService.updateUserWithRetries(
        (u) -> {
          u.setClusterConfigDefault(override);
          return u;
        },
        user);
    userService.logAdminUserAction(
        user.getUserId(), "cluster config override", oldOverride, new Gson().toJson(override));
    return ResponseEntity.ok(new EmptyResponse());
  }

  private static String clusterNameForUser(User user) {
    return "all-of-us-" + user.getUserId();
  }

  private String jsonToDataUri(JSONObject json) {
    return DATA_URI_PREFIX + Base64.getUrlEncoder().encodeToString(json.toString().getBytes());
  }

  private String aouConfigDataUri(
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace,
      CdrVersionEntity cdrVersionEntity,
      String cdrBillingCloudProject) {
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
    config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersionEntity.getBigqueryProject());
    config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersionEntity.getBigqueryDataset());
    config.put(BILLING_CLOUD_PROJECT, cdrBillingCloudProject);
    return jsonToDataUri(config);
  }
}
