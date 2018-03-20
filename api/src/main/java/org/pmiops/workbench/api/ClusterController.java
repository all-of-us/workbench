package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  private static final String DEFAULT_CLUSTER_NAME = "all-of-us";
  private static final String CLUSTER_LABEL_AOU = "all-of-us";
  private static final String CLUSTER_LABEL_CREATED_BY = "created-by";

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
  private final Provider<User> userProvider;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  ClusterController(NotebooksService notebooksService,
      Provider<User> userProvider,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
    String workspaceBucket;
    try {
      workspaceBucket = "gs://" + fireCloudService.getWorkspace(body.getWorkspaceNamespace(), body.getWorkspaceId())
          .getWorkspace()
          .getBucketName();
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        log.log(Level.INFO, "Firecloud workspace not found", e);
        throw new NotFoundException(String.format(
            "workspace %s/%s not found or not accessible",
            body.getWorkspaceNamespace(), body.getWorkspaceId()));
      }
      throw ExceptionUtils.convertFirecloudException(e);
    }

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String workspacePath = body.getWorkspaceId();
    if (!projectName.equals(body.getWorkspaceNamespace())) {
      workspacePath = body.getWorkspaceNamespace() + ":" + body.getWorkspaceId();
    }
    String apiDir = String.join("/", "workspaces", workspacePath);
    String localDir = String.join("/", "~", apiDir);
    Map<String, String> toLocalize = body.getNotebookNames()
        .stream()
        .collect(Collectors.<String, String, String>toMap(
            name -> localDir + "/" + name,
            name -> String.join("/", workspaceBucket, "notebooks", name)));
    // TODO(calbach): Localize a delocalize config JSON file as well, once Leo supports this.
    toLocalize.put(
        localDir + "/.all_of_us_config.json",
        workspaceBucket + "/" + WorkspacesController.CONFIG_FILENAME);
    notebooksService.localize(projectName, clusterName, toLocalize);
    ClusterLocalizeResponse resp = new ClusterLocalizeResponse();
    resp.setClusterLocalDirectory(apiDir);
    return ResponseEntity.ok(resp);
  }
}
