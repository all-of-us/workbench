package org.pmiops.workbench.notebooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;
import org.pmiops.workbench.notebooks.model.MachineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {
  private static final String CLUSTER_LABEL_AOU = "all-of-us";
  private static final String CLUSTER_LABEL_CREATED_BY = "created-by";


  private static final Logger log = Logger.getLogger(NotebooksServiceImpl.class.getName());

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<NotebooksApi> notebooksApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final NotebooksRetryHandler retryHandler;

  @Autowired
  public NotebooksServiceImpl(Provider<ClusterApi> clusterApiProvider,
      Provider<NotebooksApi> notebooksApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider, NotebooksRetryHandler retryHandler) {
    this.clusterApiProvider = clusterApiProvider;
    this.notebooksApiProvider = notebooksApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.retryHandler = retryHandler;
  }

  private ClusterRequest createFirecloudClusterRequest(String userEmail) {
    Map<String, String> labels = new HashMap<String, String>();
    labels.put(CLUSTER_LABEL_AOU, "true");
    labels.put(CLUSTER_LABEL_CREATED_BY, userEmail);
    return new ClusterRequest()
        .labels(labels)
        .jupyterUserScriptUri(workbenchConfigProvider.get().firecloud.jupyterUserScriptUri)
        .machineConfig(new MachineConfig()
            .masterDiskSize(20 /* GB */)
            .masterMachineType("n1-standard-1"));
  }

  @Override
  public Cluster createCluster(String googleProject, String clusterName, String userEmail) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) ->
        clusterApi.createCluster(googleProject, clusterName, createFirecloudClusterRequest(userEmail)));
  }

  @Override
  public void deleteCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    retryHandler.run((context) -> {
      clusterApi.deleteCluster(googleProject, clusterName);
      return null;
    });
  }

  @Override
  public List<Cluster> listClusters(String labels, boolean includeDeleted) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) -> clusterApi.listClusters(labels, includeDeleted));
  }

  @Override
  public Cluster getCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    return retryHandler.run((context) -> clusterApi.getCluster(googleProject, clusterName));
  }

  @Override
  public void localize(String googleProject, String clusterName, Map<String, String> fileList) {
    NotebooksApi notebooksApi = notebooksApiProvider.get();
    retryHandler.run((context) -> {
      notebooksApi.proxyLocalize(googleProject, clusterName, fileList, /* async */ false);
      return null;
    });
  }

  @Override
  public boolean getNotebooksStatus() {
    try {
      new StatusApi().getSystemStatus();
    } catch (ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      log.log(Level.WARNING, "notebooks status check request failed", e);
      return false;
    }
    return true;
  }
}
