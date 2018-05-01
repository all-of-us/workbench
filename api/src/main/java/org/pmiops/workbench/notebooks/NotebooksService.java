package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.notebooks.model.Cluster;

/**
 * Encapsulate Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface NotebooksService {
  String DEFAULT_CLUSTER_NAME = "all-of-us";

  /**
   * Creates a notebooks cluster.
   * @param googleProject the google project that will be used for this notebooks cluster
   * @param clusterName the user assigned/auto-generated name for this notebooks cluster
   * @param userEmail a string containing the user who is creating the cluster's email
   */
  Cluster createCluster(String googleProject, String clusterName, String userEmail)
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
   * Write the given file contents on a notebook Cluster. The given jupyterPath
   * is a Jupyter "API path", meaning it is relative to the root of the Jupyter
   * user directory. For example, specifying "foo.txt" would write a file which
   * would appear at the root directory in the Jupyter UI.
   */
  void putFile(String googleProject, String clusterName, String workspaceDir, String fileName, String fileContents);
  void putRootWorkspacesDir(String googleProject, String clusterName);
  void putWorkspaceDir(String googleProject, String clusterName, String workspaceDir);

  /**
   * @return true if notebooks is okay, false if notebooks are down.
   */
  boolean getNotebooksStatus();
}
