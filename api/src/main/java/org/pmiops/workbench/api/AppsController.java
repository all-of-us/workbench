package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.apache.arrow.util.VisibleForTesting;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListAppsResponse;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppsController implements AppsApiDelegate {
  private final LeonardoApiClient leonardoApiClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceService workspaceService;
  private final LeonardoApiHelper leonardoApiHelper;

  @Autowired
  public AppsController(
      LeonardoApiClient leonardoApiClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceService workspaceService,
      LeonardoApiHelper leonardoApiHelper) {
    this.leonardoApiClient = leonardoApiClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceService = workspaceService;
    this.leonardoApiHelper = leonardoApiHelper;
  }

  @Override
  public ResponseEntity<EmptyResponse> createApp(
      String workspaceNamespace, CreateAppRequest createAppRequest) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, dbWorkspace.getFirecloudName());
    validateCanPerformApiAction(dbWorkspace);

    leonardoApiClient.createApp(createAppRequest, dbWorkspace);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteApp(
      String workspaceNamespace, String appName, Boolean deleteDisk) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    leonardoApiClient.deleteApp(appName, dbWorkspace, deleteDisk);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<UserAppEnvironment> getApp(String workspaceNamespace, String appName) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    return ResponseEntity.ok(
        leonardoApiClient.getAppByNameByProjectId(dbWorkspace.getGoogleProject(), appName));
  }

  @Override
  public ResponseEntity<EmptyResponse> updateApp(
      String workspaceNamespace, String appName, UserAppEnvironment app) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<ListAppsResponse> listAppsInWorkspace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    ListAppsResponse response = new ListAppsResponse();
    response.addAll(
        leonardoApiClient.listAppsInProjectCreatedByCreator(dbWorkspace.getGoogleProject()));
    return ResponseEntity.ok(response);
  }

  /**
   * Validates user is allowed to perform User App actions.
   *
   * <p>User App actions require:
   *
   * <ul>
   *   <li>User compute is not suspended due to security reasons (e.g. egress alert)
   *   <li>User is OWNER or WRITER of the workspace
   * </ul>
   */
  @VisibleForTesting
  protected void validateCanPerformApiAction(DbWorkspace dbWorkspace) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);
    String workspaceNamespace = dbWorkspace.getWorkspaceNamespace();
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
  }
}
