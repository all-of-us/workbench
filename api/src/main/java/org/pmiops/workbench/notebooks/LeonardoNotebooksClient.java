package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.notebooks.model.StorageLink;

/**
 * Encapsulate Leonardo's Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface LeonardoNotebooksClient {
  /** lists all notebook clusters as the appengine SA, to be used only for admin operations */
  List<LeonardoListRuntimeResponse> listRuntimesByProjectAsService(String googleProject);

  /**
   * Creates a notebooks cluster owned by the current authenticated user.
   *
   * @param googleProject the google project that will be used for this notebooks runtime
   * @param runtimeName the user assigned/auto-generated name for this notebooks runtime
   * @param workspaceFirecloudName the firecloudName of the workspace this cluster is associated
   *     with
   */
  void createRuntime(String googleProject, String runtimeName, String workspaceFirecloudName)
      throws WorkbenchException;

  /** Deletes a notebook runtime */
  void deleteRuntime(String googleProject, String runtimeName) throws WorkbenchException;

  /** Deletes a notebook runtime as the appengine SA, to be used only for admin operations */
  void deleteRuntimeAsService(String googleProject, String runtimeName) throws WorkbenchException;

  /** Gets information about a notebook runtime */
  LeonardoGetRuntimeResponse getRuntime(String googleProject, String runtimeName)
      throws WorkbenchException;

  /** Send files over to notebook runtime */
  void localize(String googleProject, String runtimeName, Map<String, String> fileList)
      throws WorkbenchException;

  /** Create a new data synchronization Welder storage link on a runtime. */
  StorageLink createStorageLink(String googleProject, String runtime, StorageLink storageLink);

  /** @return true if Leonardo service is okay, false otherwise. */
  boolean getLeonardoStatus();
}
