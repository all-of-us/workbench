package org.pmiops.workbench.api;

import java.time.Clock;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.App;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListAppsResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController implements AppApiDelegate {
  private final LeonardoApiClient leonardoApiClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public AppController(Clock clock,
      LeonardoApiClient leonardoApiClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserRecentResourceService userRecentResourceService) {
    this.leonardoApiClient = leonardoApiClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<EmptyResponse> createApp(String workspaceNamespace, App app) {
    if(!workbenchConfigProvider.get().featureFlags.enableGkeApp) {
      throw new UnsupportedOperationException("API not supported.");
    }
    DbUser user = userProvider.get();
    enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

    leonardoApiClient.createApp(app, workspaceNamespace, firecloudWorkspaceName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteApp(String workspaceNamespace, Boolean deleteDisk) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<App> getApp(String workspaceNamespace) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<EmptyResponse> updateApp(String workspaceNamespace, App app) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<ListAppsResponse> listApp() {
    throw new UnsupportedOperationException("API not supported.");
  }
}
