package org.pmiops.workbench.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  private final NotebooksService notebooksService;
  private final Provider<User> userProvider;
  private final WorkspaceService workspaceService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster> TO_ALL_OF_US_CLUSTER =
    new Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster>() {
      @Override
      public Cluster apply(org.pmiops.workbench.notebooks.model.Cluster firecloudCluster) {
        Cluster allOfUsCluster = new Cluster();
        allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
        allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject());
        // TODO(calbach): Use an enum in the Workbench notebooks API.
        allOfUsCluster.setStatus(firecloudCluster.getStatus().toString());
        allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
        allOfUsCluster.setDestroyedDate(firecloudCluster.getDestroyedDate());
        allOfUsCluster.setLabels(firecloudCluster.getLabels());
        return allOfUsCluster;
      }
    };

  private org.pmiops.workbench.notebooks.model.ClusterRequest createFirecloudClusterRequest() {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    Map<String, String> labels = new HashMap<String, String>();
    labels.put("all-of-us", "true");
    labels.put("created-by", userProvider.get().getEmail());
    firecloudClusterRequest.setLabels(labels);
    // TODO: Host our extension somewhere.
    // firecloudClusterRequest.setJupyterExtensionUri("");
    firecloudClusterRequest.setJupyterUserScriptUri(
        workbenchConfigProvider.get().firecloud.jupyterUserScriptUri);

    return firecloudClusterRequest;
  }

  private String convertClusterName(String workspaceId) {
      String clusterName = workspaceId + this.userProvider.get().getUserId();
      clusterName = clusterName.toLowerCase();
      return clusterName;
  }

  @Autowired
  ClusterController(NotebooksService notebooksService,
      Provider<User> userProvider,
      WorkspaceService workspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<Cluster> createCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);

    String clusterName = this.convertClusterName(workspaceId);
    Cluster createdCluster = TO_ALL_OF_US_CLUSTER.apply(
        this.notebooksService.createCluster(
            workspaceNamespace, clusterName, createFirecloudClusterRequest()));
    return ResponseEntity.ok(createdCluster);
  }


  @Override
  public ResponseEntity<EmptyResponse> deleteCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);

    String clusterName = this.convertClusterName(workspaceId);
    this.notebooksService.deleteCluster(workspaceNamespace, clusterName);
    return ResponseEntity.ok(new EmptyResponse());
  }


  @Override
  public ResponseEntity<Cluster> getCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);

    String clusterName = this.convertClusterName(workspaceId);
    Cluster cluster = TO_ALL_OF_US_CLUSTER.apply(
        this.notebooksService.getCluster(workspaceNamespace, clusterName));
    return ResponseEntity.ok(cluster);
  }


  @Override
  public ResponseEntity<ClusterListResponse> listClusters(String labels) {
    List<org.pmiops.workbench.notebooks.model.Cluster> oldClusters;
    oldClusters = this.notebooksService.listClusters(labels, /* includeDeleted */ false);
    ClusterListResponse response = new ClusterListResponse();
    response.setItems(oldClusters.stream().map(TO_ALL_OF_US_CLUSTER).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> localizeNotebook(String workspaceNamespace, String workspaceId,
      List<FileDetail> fileList) {
    String clusterName = convertClusterName(workspaceId);
    this.notebooksService.localize(workspaceNamespace, clusterName,
        convertfileDetailsToMap(fileList));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Create a map with key as ~/fileName and value as file path which should be in format the gs://firecloudBucket name/filename
   * @param fileList
   * @return FileDetail Map
   */
  private Map<String, String> convertfileDetailsToMap(List<FileDetail> fileList) {
    return fileList.stream()
        .collect(Collectors.toMap(fileDetail -> "~/" + fileDetail.getName(),
            fileDetail -> fileDetail.getPath()));
  }
}
