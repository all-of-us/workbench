package org.pmiops.workbench.api;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterRequest;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  // This will currently only work inside the Broad's network.
  private static final Logger log = Logger.getLogger(BugReportController.class.getName());

  private final NotebooksService notebooksService;
  private final Provider<User> userProvider;

  private Cluster toAllOfUsCluster(org.pmiops.workbench.notebooks.model.Cluster firecloudCluster) {
    Cluster allOfUsCluster = new Cluster();
    allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
    allOfUsCluster.setGoogleId(firecloudCluster.getGoogleId());
    allOfUsCluster.setGoogleProject(firecloudCluster.getGoogleProject());
    allOfUsCluster.setGoogleServiceAccount(firecloudCluster.getGoogleServiceAccount());
    allOfUsCluster.setGoogleBucket(firecloudCluster.getGoogleBucket());
    allOfUsCluster.setOperationName(firecloudCluster.getOperationName());
    allOfUsCluster.setStatus(firecloudCluster.getStatus());
    allOfUsCluster.setHostIp(firecloudCluster.getHostIp());
    allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
    allOfUsCluster.setDestroyedDate(firecloudCluster.getDestroyedDate());
    allOfUsCluster.setLabels(firecloudCluster.getLabels());

    return allOfUsCluster;
  }

  private org.pmiops.workbench.notebooks.model.ClusterRequest toFirecloudClusterRequest(ClusterRequest allOfUsClusterRequest) {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    firecloudClusterRequest.setBucketPath(allOfUsClusterRequest.getBucketPath());
    firecloudClusterRequest.setServiceAccount(allOfUsClusterRequest.getServiceAccount());
    firecloudClusterRequest.setLabels(allOfUsClusterRequest.getLabels());
    firecloudClusterRequest.setJupyterExtensionUri(allOfUsClusterRequest.getJupyterExtensionUri());

    return firecloudClusterRequest;
  }

  private String convertClusterName(String workspaceId) {
      String clusterName = workspaceId + this.userProvider.get().getUserId();
      clusterName = clusterName.toLowerCase();
      return clusterName;
  }

  @Autowired
  ClusterController(NotebooksService notebooksService,
      Provider<User> userProvider) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
  }

  public ResponseEntity<Cluster> createCluster(String workspaceNamespace,
      String workspaceId,
      ClusterRequest clusterRequest) {
    Cluster createdCluster;

    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      createdCluster = toAllOfUsCluster(this.notebooksService.createCluster("broad-dsde-dev", clusterName, toFirecloudClusterRequest(clusterRequest)));
    } catch (ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(createdCluster);
  }


  public ResponseEntity<EmptyResponse> deleteCluster(String workspaceNamespace,
      String workspaceId) {
    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      this.notebooksService.deleteCluster("broad-dsde-dev", clusterName);
    } catch (ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    EmptyResponse e = new EmptyResponse();
    return ResponseEntity.ok(e);
  }


  public ResponseEntity<Cluster> getCluster(String workspaceNamespace,
      String workspaceId) {
    String clusterName = this.convertClusterName(workspaceId);
    Cluster cluster;
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      cluster = toAllOfUsCluster(this.notebooksService.getCluster("broad-dsde-dev", clusterName));
    } catch(ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(cluster);
  }


  public ResponseEntity<List<Cluster>> listClusters(String labels) {
    List<Cluster> newClusters = new ArrayList<Cluster>();
    List<org.pmiops.workbench.notebooks.model.Cluster> oldClusters;
    try {
      oldClusters = this.notebooksService.listClusters(labels);
    } catch(ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    for (org.pmiops.workbench.notebooks.model.Cluster c : oldClusters) {
      newClusters.add(toAllOfUsCluster(c));
    }
    return ResponseEntity.ok(newClusters);
  }
}
