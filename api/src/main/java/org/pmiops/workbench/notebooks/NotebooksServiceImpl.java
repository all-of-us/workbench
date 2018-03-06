package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<NotebooksApi> notebooksApiProvider;

  @Autowired
  public NotebooksServiceImpl(Provider<ClusterApi> clusterApiProvider,
      Provider<NotebooksApi> notebooksApiProvider) {
    this.clusterApiProvider = clusterApiProvider;
    this.notebooksApiProvider = notebooksApiProvider;
  }

  @Override
  public Cluster createCluster(String googleProject, String clusterName, ClusterRequest clusterRequest) {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.createCluster(googleProject, clusterName, clusterRequest);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
  }

  @Override
  public void deleteCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      clusterApi.deleteCluster(googleProject, clusterName);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
  }

  @Override
  public List<Cluster> listClusters(String labels, boolean includeDeleted) {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.listClusters(labels, includeDeleted);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
  }

  @Override
  public Cluster getCluster(String googleProject, String clusterName) {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.getCluster(googleProject, clusterName);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
  }

  @Override
  public void localize(String googleProject, String clusterName, Map<String, String> fileList) {
    NotebooksApi notebooksApi = notebooksApiProvider.get();
    try {
      notebooksApi.proxyLocalize(googleProject, clusterName, fileList);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }
  }

  @Override
  public boolean getNotebooksStatus() {
    try {
      new StatusApi().getSystemStatus();
    } catch (ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      return false;
    }
    return true;
  }
}
