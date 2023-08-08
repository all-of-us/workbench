package org.pmiops.workbench.leonardo;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.notebooks.model.StorageLink;

/**
 * Encapsulate Leonardo's Notebooks API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface LeonardoApiClient {
  /** lists all notebook runtimes as the appengine SA, to be used only for admin operations */
  List<ListRuntimeResponse> listRuntimesByProjectAsService(String googleProject);

  List<ListRuntimeResponse> listRuntimesByProject(
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
  void deleteRuntime(String googleProject, String runtimeName, Boolean deleteDisk)
      throws WorkbenchException;

  /** Deletes a notebook runtime as the appengine SA, to be used only for admin operations */
  void deleteRuntimeAsService(String googleProject, String runtimeName) throws WorkbenchException;

  /**
   * Stops all runtimes created by the user, if any can be stopped. Returns the count of stopped
   * runtimes.
   */
  int stopAllUserRuntimesAsService(String userEmail) throws WorkbenchException;

  /** Gets information about a notebook runtime */
  GetRuntimeResponse getRuntime(String googleProject, String runtimeName)
      throws WorkbenchException;

  /** Send files over to notebook runtime */
  void localizeForRuntime(String googleProject, String runtimeName, Map<String, String> fileList)
      throws WorkbenchException;

  /** Create a new data synchronization Welder storage link on a runtime. */
  StorageLink createStorageLinkForRuntime(
      String googleProject, String runtime, StorageLink storageLink);

  /** Send files over to Gke APP */
  void localizeForApp(String googleProject, String appName, Map<String, String> fileList)
      throws WorkbenchException;

  /** Create a new data synchronization Welder storage link on a Gke APP. */
  StorageLink createStorageLinkForApp(
      String googleProject, String appName, StorageLink storageLink);

  /** Gets information about a persistent disk */
  GetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
      throws WorkbenchException;

  /** Deletes a persistent disk */
  void deletePersistentDisk(String googleProject, String diskName) throws WorkbenchException;

  /** Deletes a persistent disk as an admin */
  void deletePersistentDiskAsService(String googleProject, String diskName)
      throws WorkbenchException;

  /** Update a persistent disk */
  void updatePersistentDisk(String googleProject, String diskName, Integer diskSize)
      throws WorkbenchException;

  /** List all persistent disks owned by authenticated user in google project */
  List<ListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject);

  /**
   * Creates Leonardo app owned by the current authenticated user.
   *
   * @param createAppRequest the details for the app to create
   * @param dbWorkspace the workspace to create App within.
   */
  void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace)
      throws WorkbenchException;

  /**
   * Gets Leonardo app owned by app name and workspace GCP project
   *
   * @param googleProjectId the GCP project the app belongs to
   * @param appName the name of the app
   */
  UserAppEnvironment getAppByNameByProjectId(String googleProjectId, String appName);

  /**
   * Lists all apps the user creates in the given workspace GCP project
   *
   * @param googleProjectId the GCP project the app belongs to
   */
  List<UserAppEnvironment> listAppsInProjectCreatedByCreator(String googleProjectId);

  List<UserAppEnvironment> listAppsInProjectAsService(String googleProjectId);

  /**
   * Deletes a Leonardo app
   *
   * @param appName the name of the app
   * @param dbWorkspace the workspace to delete the app in
   * @param deleteDisk whether the app's persistent disk should also be deleted
   */
  void deleteApp(String appName, DbWorkspace dbWorkspace, boolean deleteDisk)
      throws WorkbenchException;

  /** @return true if Leonardo service is okay, false otherwise. */
  boolean getLeonardoStatus();

  int stopAllUserAppsAsService(String userEmail);

  /** List all persistent disks in google project */
  List<ListPersistentDiskResponse> listDisksByProjectAsService(String googleProject);
}
