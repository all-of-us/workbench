package org.pmiops.workbench.app;

import java.util.List;
import javax.inject.Provider;
import org.apache.arrow.util.VisibleForTesting;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class AppsServiceImpl implements AppsService {

  private final LeonardoApiClient leonardoApiClient;

  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final LeonardoApiHelper leonardoApiHelper;

  @Autowired
  public AppsServiceImpl(
      LeonardoApiClient leonardoApiClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      LeonardoApiHelper leonardoApiHelper) {
    this.leonardoApiClient = leonardoApiClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.leonardoApiHelper = leonardoApiHelper;
  }

  @Override
  public void createApp(CreateAppRequest createAppRequest, DbWorkspace dbWorkspace) {
    leonardoApiClient.createApp(createAppRequest, dbWorkspace);
  }

  @Override
  public void deleteApp(String appName, DbWorkspace dbWorkspace, Boolean deleteDisk) {
    leonardoApiClient.deleteApp(appName, dbWorkspace, deleteDisk);
  }

  @Override
  public UserAppEnvironment getApp(String appName, DbWorkspace dbWorkspace) {
    return leonardoApiClient.getAppByNameByProjectId(dbWorkspace.getGoogleProject(), appName);
  }

  @Override
  public void updateApp(String appName, UserAppEnvironment app, DbWorkspace dbWorkspace) {
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public List<UserAppEnvironment> listAppsInWorkspace(DbWorkspace dbWorkspace) {
    validateCanPerformApiAction(dbWorkspace);
    return leonardoApiClient.listAppsInProjectCreatedByCreator(dbWorkspace.getGoogleProject());
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
  // FIXME for multicloud
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
