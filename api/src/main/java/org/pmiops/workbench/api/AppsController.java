package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.App;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListAppsResponse;
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
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceService workspaceService;
  private final LeonardoApiHelper leonardoApiHelper;

  @Autowired
  public AppsController(
      LeonardoApiClient leonardoApiClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceService workspaceService,
      LeonardoApiHelper leonardoApiHelper) {
    this.leonardoApiClient = leonardoApiClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceService = workspaceService;
    this.leonardoApiHelper = leonardoApiHelper;
  }

  @Override
  public ResponseEntity<EmptyResponse> createApp(
      String workspaceNamespace, CreateAppRequest createAppRequest) {
    if (!workbenchConfigProvider.get().featureFlags.enableGkeApp) {
      throw new UnsupportedOperationException("API not supported.");
    }
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    leonardoApiClient.createApp(createAppRequest, dbWorkspace);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteApp(
      String workspaceNamespace, String appName, Boolean deleteDisk) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<App> getApp(String workspaceNamespace, String appName) {
    if (!workbenchConfigProvider.get().featureFlags.enableGkeApp) {
      throw new UnsupportedOperationException("API not supported.");
    }
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    return ResponseEntity.ok(
        leonardoApiClient.getAppByNameByProjectId(dbWorkspace.getGoogleProject(), appName));
  }

  @Override
  public ResponseEntity<EmptyResponse> updateApp(
      String workspaceNamespace, String appName, App app) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<ListAppsResponse> listAppsInWorkspace(String workspaceNamespace) {
    if (!workbenchConfigProvider.get().featureFlags.enableGkeApp) {
      throw new UnsupportedOperationException("API not supported.");
    }
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    validateCanPerformApiAction(dbWorkspace);

    ListAppsResponse response = new ListAppsResponse();
    response.addAll(leonardoApiClient.listAppsInProject(dbWorkspace.getGoogleProject()));
    return ResponseEntity.ok(response);
  }

  /** */
  private void validateCanPerformApiAction(DbWorkspace dbWorkspace) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);
    String workspaceNamespace = dbWorkspace.getWorkspaceNamespace();
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);
  }
}
