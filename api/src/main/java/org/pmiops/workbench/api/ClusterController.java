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
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  private static final Map<org.pmiops.workbench.notebooks.model.ClusterStatus, ClusterStatus> fcToWorkbenchStatusMap =
      new ImmutableMap.Builder<org.pmiops.workbench.notebooks.model.ClusterStatus, ClusterStatus>()
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.CREATING, ClusterStatus.CREATING)
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.RUNNING, ClusterStatus.RUNNING)
      .put(org.pmiops.workbench.notebooks.model.ClusterStatus.ERROR, ClusterStatus.ERROR)
      .build();

  private static final Logger log = Logger.getLogger(ClusterController.class.getName());

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

  /**
   * Deterministically produces a cluster name for a given user/environment.
   * Clusters are namespaced under a free tier project, which is unique to each
   * user, therefore the only true requirements here are to:
   * 1. have a reproducible mapping of user -> cluster name (as we currently
   *    don't store this)
   * 2. generate a unique name across AoU environments (decision still pending)
   */
  private String clusterNameForUser() {
      String id =
          workbenchConfigProvider.get().server.projectId + this.userProvider.get().getEmail();
      // Take the last 4 digits of the hash of the above.
      return "aou-" + String.format("%04x", id.hashCode()).substring(0, 4);
  }

  @Override
  public ResponseEntity<ClusterListResponse> listClusters() {
    String project = userProvider.get().getFreeTierBillingProjectName();
    String clusterName = this.clusterNameForUser();

    org.pmiops.workbench.notebooks.model.Cluster fcCluster;
    try {
      fcCluster = this.notebooksService.getCluster(project, clusterName);
    } catch (NotFoundException e) {
      fcCluster = this.notebooksService.createCluster(project, clusterName, createFirecloudClusterRequest());
    }
    ClusterListResponse resp = new ClusterListResponse();
    resp.setDefaultCluster(TO_ALL_OF_US_CLUSTER.apply(fcCluster));
    return ResponseEntity.ok(resp);
  }

  private org.pmiops.workbench.notebooks.model.ClusterRequest createFirecloudClusterRequest() {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    // TODO(calbach): Configure Jupyter server extensions here.
    Map<String, String> labels = new HashMap<String, String>();
    labels.put("all-of-us", "true");
    labels.put("created-by", userProvider.get().getEmail());
    firecloudClusterRequest.setLabels(labels);
    return firecloudClusterRequest;
  }

  @Override
  public ResponseEntity<Void> localize(
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
    String localDir = String.join("/", "~", "workspaces", workspacePath);
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
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
