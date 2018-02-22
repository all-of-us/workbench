package org.pmiops.workbench.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.ApiException;
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

  private static final Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster> TO_ALL_OF_US_CLUSTER =
    new Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster>() {
      @Override
      public Cluster apply(org.pmiops.workbench.notebooks.model.Cluster firecloudCluster) {
        Cluster allOfUsCluster = new Cluster();
        allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
        allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject());
        allOfUsCluster.setStatus(firecloudCluster.getStatus());
        allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
        allOfUsCluster.setDestroyedDate(firecloudCluster.getDestroyedDate());
        allOfUsCluster.setLabels(firecloudCluster.getLabels());
        return allOfUsCluster;
      }
    };

  private org.pmiops.workbench.notebooks.model.ClusterRequest createFirecloudClusterRequest() {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    // TODO: Use real paths and accounts.
    firecloudClusterRequest.setBucketPath("");
    firecloudClusterRequest.setServiceAccount("");
    Map<String, String> labels = new HashMap<String, String>();
    labels.put("all-of-us", "true");
    labels.put("created-by", userProvider.get().getEmail());
    firecloudClusterRequest.setLabels(labels);
    // TODO: Host our extension somewhere.
    // firecloudClusterRequest.setJupyterExtensionUri("");

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
      WorkspaceService workspaceService) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Cluster> createCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);


    Cluster createdCluster;

    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      createdCluster = TO_ALL_OF_US_CLUSTER.apply(this.notebooksService.createCluster(workspaceNamespace, clusterName, createFirecloudClusterRequest()));
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
    return ResponseEntity.ok(createdCluster);
  }


  @Override
  public ResponseEntity<EmptyResponse> deleteCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);

    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      this.notebooksService.deleteCluster(workspaceNamespace, clusterName);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
    EmptyResponse e = new EmptyResponse();
    return ResponseEntity.ok(e);
  }


  @Override
  public ResponseEntity<Cluster> getCluster(String workspaceNamespace,
      String workspaceId) {

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);

    String clusterName = this.convertClusterName(workspaceId);
    Cluster cluster;
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      cluster = TO_ALL_OF_US_CLUSTER.apply(this.notebooksService.getCluster(workspaceNamespace, clusterName));
    } catch(ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
    return ResponseEntity.ok(cluster);
  }


  @Override
  public ResponseEntity<ClusterListResponse> listClusters(String labels) {
    List<org.pmiops.workbench.notebooks.model.Cluster> oldClusters;
    try {
      oldClusters = this.notebooksService.listClusters(labels);
    } catch(ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
    ClusterListResponse response = new ClusterListResponse();
    response.setItems(oldClusters.stream().map(TO_ALL_OF_US_CLUSTER).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> localizeNotebook(String workspaceNamespace, String workspaceId,
      List<FileDetail> fileList) {
    try {
      String clusterName = convertClusterName(workspaceId);
      this.notebooksService.localize(workspaceNamespace, clusterName,
          convertfileDetailsToMap(fileList));
    } catch (ApiException e) {
      if (e.getCode() == 400) {
        throw new BadRequestException(e.getResponseBody());
      } else if (e.getCode() == 404) {
        throw new NotFoundException("Cluster not found.");
      }
      throw new ServerErrorException(e);
    }
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
