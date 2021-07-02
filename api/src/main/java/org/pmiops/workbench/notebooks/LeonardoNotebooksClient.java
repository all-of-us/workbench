package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.notebooks.model.StorageLink;

/**
 * Encapsulate Leonardo's Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface LeonardoNotebooksClient {

  /** lists all notebook runtimes as the appengine SA, to be used only for admin operations */
  List<LeonardoListRuntimeResponse> listRuntimesByProjectAsService(String googleProject);

  List<LeonardoListRuntimeResponse> listRuntimesByProject(
      String googleProject, boolean includeDeleted);

  /**
   * Creates a notebooks runtime owned by the current authenticated user.
   *
   * @param runtime the details for the runtime to create
   * @param workspaceNamespace the workspace namespace to identify a workspace.
   * @param workspaceFirecloudName the firecloudName of the workspace this runtime is associated
   *     with
   */
  void createRuntime(Runtime runtime, String workspaceNamespace, String workspaceFirecloudName)
      throws WorkbenchException;

  void updateRuntime(Runtime runtime) throws WorkbenchException;

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

  void createPersistentDisk(String googleProject, String diskName, Integer diskSize, LeonardoDiskType diskType, Integer blockSize)
          throws WorkbenchException;

  LeonardoGetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
          throws WorkbenchException;

  /** Deletes a notebook runtime */
  void deletePersistentDisk(String googleProject, String diskName) throws WorkbenchException;

  void updatePersistentDisk(String googleProject, String diskName, Integer diskSize, LeonardoDiskType diskType, Integer blockSize)
          throws WorkbenchException;

  /** @return true if Leonardo service is okay, false otherwise. */
  boolean getLeonardoStatus();
}
