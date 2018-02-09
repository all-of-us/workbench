package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.StatusApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<StatusApi> statusApiProvider;
  private final Provider<User> userProvider;

  @Autowired
  public NotebooksServiceImpl(Provider<ClusterApi> clusterApiProvider,
      Provider<StatusApi> statusApiProvider,
      Provider<User> userProvider) {
    this.clusterApiProvider = clusterApiProvider;
    this.statusApiProvider = statusApiProvider;
    this.userProvider = userProvider;
  }

  @Override
  public Cluster createCluster(String googleProject, String clusterName, ClusterRequest clusterRequest)
      throws ApiException {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.createCluster(googleProject, clusterName, clusterRequest);
    } catch (ApiException e) {
      // TODO: Handle notebook cluster creation exceptions cleanly.
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteCluster(String googleProject, String clusterName) throws ApiException {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      clusterApi.deleteCluster(googleProject, clusterName);
    } catch (ApiException e) {
      // TODO: Handle notebook cluster deletion exceptions cleanly.
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Cluster> listClusters(String labels) throws ApiException {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.listClusters(labels);
    } catch (ApiException e) {
      // TODO: Handle list clusters exceptions cleanly
      throw new RuntimeException(e);
    }
  }

  @Override
  public Cluster getCluster(String googleProject, String clusterName) throws ApiException {
    ClusterApi clusterApi = clusterApiProvider.get();
    try {
      return clusterApi.getCluster(googleProject, clusterName);
    } catch (ApiException e) {
      // TODO: Handle get cluster exceptions cleanly
      throw new RuntimeException(e);
    }
  }

  @Override
  public void localize(String googleProject, String clusterName, Map fileList) throws ApiException {
    ClusterApi clusterApi = clusterApiProvider.get();
    clusterApi.localizeFiles(googleProject, clusterName, fileList);
  }

  @Override
  public boolean getNotebooksStatus() {
    StatusApi statusApi = statusApiProvider.get();
    try {
      statusApi.status();
    } catch (ApiException e) {
      // If any of the systems for notebooks are down, it won't work for us.
      return false;
    }
    return true;
  }
}
