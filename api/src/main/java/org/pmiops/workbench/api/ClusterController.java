package org.pmiops.workbench.api;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterRequest;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());

  private final NotebooksService notebooksService;

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


  @Autowired
  ClusterController(NotebooksService notebooksService) {
    this.notebooksService = notebooksService;
  }

  public ResponseEntity<Cluster> createCluster(String googleProject,
      String clusterName,
      ClusterRequest clusterRequest) {
    Cluster createdCluster;
    try {
      createdCluster = toAllOfUsCluster(this.notebooksService.createCluster(googleProject, clusterName, toFirecloudClusterRequest(clusterRequest)));
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(createdCluster);
  }


  public ResponseEntity<Void> deleteCluster(String googleProject,
      String clusterName) {
    try {
      this.notebooksService.deleteCluster(googleProject, clusterName);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(null);
  }


  public ResponseEntity<Cluster> getCluster(String googleProject,
      String clusterName) {
    Cluster cluster;
    try {
      cluster = toAllOfUsCluster(this.notebooksService.getCluster(googleProject, clusterName));
    } catch(ApiException e) {
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
      throw new RuntimeException(e);
    }
    for (org.pmiops.workbench.notebooks.model.Cluster c : oldClusters) {
      newClusters.add(toAllOfUsCluster(c));
    }
    return ResponseEntity.ok(newClusters);
  }
}
