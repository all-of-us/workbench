package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterRequest;

/**
 * Encapsulate Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface NotebooksService {

  /**
   * Creates a notebooks cluster.
   * @param googleProject the google project that will be used for this notebooks cluster
   * @param clusterName the user assigned/auto-generated name for this notebooks cluster
   * @param clusterRequest an object with information about the google bucket and user service account credentials.
   */
  Cluster createCluster(String googleProject, String clusterName, ClusterRequest clusterRequest)
      throws WorkbenchException;

  /**
   * Deletes a notebook cluster
   */
  void deleteCluster(String googleProject, String clusterName) throws WorkbenchException;

  /**
   * Lists all existing clusters
   */
  List<Cluster> listClusters(String labels, boolean includeDeleted) throws WorkbenchException;

  /**
   * Gets information about a notebook cluster
   */
  Cluster getCluster(String googleProject, String clusterName) throws WorkbenchException;

  /**
   * Send files over to notebook Cluster
   */
  void localize(String googleProject, String clusterName, Map<String, String> fileList)
      throws WorkbenchException;

  /**
   * @return true if notebooks is okay, false if notebooks are down.
   */
  boolean getNotebooksStatus();
}
