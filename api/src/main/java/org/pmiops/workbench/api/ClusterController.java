package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  private static final String DEFAULT_CLUSTER_NAME = "all-of-us";
  private static final String CLUSTER_LABEL_AOU = "all-of-us";
  private static final String CLUSTER_LABEL_CREATED_BY = "created-by";

  // Writing this file to a directory on a Leonardo cluster will result in
  // delocalization of saved files back to a given GCS location. See
  // https://github.com/DataBiosphere/leonardo/blob/develop/jupyter-docker/jupyter_delocalize.py#L12
  private static final String DELOCALIZE_CONFIG_FILENAME = ".delocalize.json";

  // This file is used by the All of Us libraries to access workspace/CDR metadata.
  private static final String AOU_CONFIG_FILENAME = ".all_of_us_config.json";
  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_ID_KEY = "WORKSPACE_ID";
  private static final String API_HOST_KEY = "API_HOST";
  private static final String BUCKET_NAME_KEY = "BUCKET_NAME";
  private static final String CDR_VERSION_CLOUD_PROJECT = "CDR_VERSION_CLOUD_PROJECT";
  private static final String CDR_VERSION_BIGQUERY_DATASET = "CDR_VERSION_BIGQUERY_DATASET";

  private static final Logger log = Logger.getLogger(ClusterController.class.getName());

  private static final Map<org.pmiops.workbench.notebooks.model.ClusterStatus, ClusterStatus> fcToWorkbenchStatusMap =
      new ImmutableMap.Builder<org.pmiops.workbench.notebooks.model.ClusterStatus, ClusterStatus>()
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.CREATING, ClusterStatus.CREATING)
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING, ClusterStatus.RUNNING)
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.ERROR, ClusterStatus.ERROR)
      .build();

  private static final Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster> TO_ALL_OF_US_CLUSTER =
    new Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster>() {
      @Override
      public Cluster apply(org.pmiops.workbench.notebooks.model.Cluster firecloudCluster) {
        Cluster allOfUsCluster = new Cluster();
        allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
        allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject());
        if (fcToWorkbenchStatusMap.containsKey(firecloudCluster.getStatus())) {
          allOfUsCluster.setStatus(fcToWorkbenchStatusMap.get(firecloudCluster.getStatus()));
        }
        allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
        return allOfUsCluster;
      }
    };

  private final NotebooksService notebooksService;
  private Provider<User> userProvider;
  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final String apiHostName;

  @Autowired
  ClusterController(NotebooksService notebooksService,
      Provider<User> userProvider,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      @Qualifier("apiHostName") String apiHostName) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.apiHostName = apiHostName;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<ClusterListResponse> listClusters() {
    String project = userProvider.get().getFreeTierBillingProjectName();

    org.pmiops.workbench.notebooks.model.Cluster fcCluster;
    try {
      fcCluster = this.notebooksService.getCluster(project, DEFAULT_CLUSTER_NAME);
    } catch (NotFoundException e) {
      fcCluster = this.notebooksService.createCluster(
          project, DEFAULT_CLUSTER_NAME, createFirecloudClusterRequest());
    }
    ClusterListResponse resp = new ClusterListResponse();
    resp.setDefaultCluster(TO_ALL_OF_US_CLUSTER.apply(fcCluster));
    return ResponseEntity.ok(resp);
  }

  private org.pmiops.workbench.notebooks.model.ClusterRequest createFirecloudClusterRequest() {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    Map<String, String> labels = new HashMap<String, String>();
    labels.put(CLUSTER_LABEL_AOU, "true");
    labels.put(CLUSTER_LABEL_CREATED_BY, userProvider.get().getEmail());
    firecloudClusterRequest.setLabels(labels);
    firecloudClusterRequest.setJupyterUserScriptUri(
        workbenchConfigProvider.get().firecloud.jupyterUserScriptUri);
    return firecloudClusterRequest;
  }

  @Override
  public ResponseEntity<ClusterLocalizeResponse> localize(
      String projectName, String clusterName, ClusterLocalizeRequest body) {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;
    try {
      fcWorkspace = fireCloudService.getWorkspace(body.getWorkspaceNamespace(), body.getWorkspaceId())
          .getWorkspace();
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        log.log(Level.INFO, "Firecloud workspace not found", e);
        throw new NotFoundException(String.format(
            "workspace %s/%s not found or not accessible",
            body.getWorkspaceNamespace(), body.getWorkspaceId()));
      }
      throw ExceptionUtils.convertFirecloudException(e);
    }
    CdrVersion cdrVersion =
        workspaceService.getRequired(body.getWorkspaceNamespace(), body.getWorkspaceId()).getCdrVersion();

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String gcsNotebooksDir = "gs://" + fcWorkspace.getBucketName() + "/notebooks";
    String workspacePath = body.getWorkspaceId();
    if (!projectName.equals(body.getWorkspaceNamespace())) {
      workspacePath = body.getWorkspaceNamespace() + ":" + body.getWorkspaceId();
    }

    // Materialize the JSON config files directly via the Jupyter server API.
    // We perform this on every localize call because the Jupyter local
    // directory can be deleted by the user, and config files may become
    // outdated, e.g. as we add new properties to the .all_of_us_config.json.
    // In most cases, aside from the first localize() call this initialization
    // work will be a no-op.
    // TODO: It is very inefficient to do 5 serial requests here. Rework this
    // to utilize "mkdir -p"-like functionality, e.g. via starting with calling
    // the /localize endpoint.
    notebooksService.putRootWorkspacesDir(projectName, clusterName);
    notebooksService.putWorkspaceDir(projectName, clusterName, workspacePath);
    notebooksService.putFile(
        projectName, clusterName, workspacePath, DELOCALIZE_CONFIG_FILENAME,
        new JSONObject()
            .put("destination", gcsNotebooksDir)
            .put("pattern", "\\.ipynb$")
            .toString());
    notebooksService.putFile(
        projectName, clusterName, workspacePath, AOU_CONFIG_FILENAME,
        aouConfigJson(fcWorkspace, cdrVersion).toString());

    // Localize the requested notebooks, if any.
    String apiDir = "workspaces/" + workspacePath;
    if (body.getNotebookNames() != null && !body.getNotebookNames().isEmpty()) {
      String localDir = "~/" + apiDir;
      notebooksService.localize(projectName, clusterName, body.getNotebookNames()
          .stream()
          .collect(Collectors.<String, String, String>toMap(
              name -> localDir + "/" + name,
              name -> gcsNotebooksDir + "/" + name)));
    }

    ClusterLocalizeResponse resp = new ClusterLocalizeResponse();
    resp.setClusterLocalDirectory(apiDir);
    return ResponseEntity.ok(resp);
  }

  private JSONObject aouConfigJson(org.pmiops.workbench.firecloud.model.Workspace fcWorkspace,
      CdrVersion cdrVersion) {
    JSONObject config = new JSONObject();

    config.put(WORKSPACE_NAMESPACE_KEY, fcWorkspace.getNamespace());
    config.put(WORKSPACE_ID_KEY, fcWorkspace.getName());
    config.put(BUCKET_NAME_KEY, fcWorkspace.getBucketName());
    config.put(API_HOST_KEY, this.apiHostName);
    config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersion.getBigqueryProject());
    config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersion.getBigqueryDataset());
    return config;
  }
}
