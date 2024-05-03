package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.model.AppLocalizeRequest;
import org.pmiops.workbench.model.AppLocalizeResponse;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListAppsResponse;
import org.pmiops.workbench.model.UserAppEnvironment;
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
  private final InteractiveAnalysisService interactiveAnalysisService;
  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public AppsController(
      LeonardoApiClient leonardoApiClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceService workspaceService,
      LeonardoApiHelper leonardoApiHelper,
      InteractiveAnalysisService interactiveAnalysisService,
      Provider<WorkbenchConfig> configProvider) {
    this.leonardoApiClient = leonardoApiClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceService = workspaceService;
    this.leonardoApiHelper = leonardoApiHelper;
    this.interactiveAnalysisService = interactiveAnalysisService;
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<EmptyResponse> createApp(
      String workspaceNamespace, CreateAppRequest createAppRequest) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, dbWorkspace.getFirecloudName());
    leonardoApiHelper.enforceComputeSecuritySuspension(userProvider.get());
    leonardoApiClient.createApp(createAppRequest, dbWorkspace);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteApp(
      String workspaceNamespace, String appName, Boolean deleteDisk) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    leonardoApiClient.deleteApp(appName, dbWorkspace, deleteDisk);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<UserAppEnvironment> getApp(String workspaceNamespace, String appName) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    return ResponseEntity.ok(
        leonardoApiClient.getAppByNameByProjectId(dbWorkspace.getGoogleProject(), appName));
  }

  @Override
  public ResponseEntity<EmptyResponse> updateApp(
      String workspaceNamespace, String appName, UserAppEnvironment app) {
    leonardoApiHelper.enforceComputeSecuritySuspension(userProvider.get());
    throw new UnsupportedOperationException("API not supported.");
  }

  @Override
  public ResponseEntity<ListAppsResponse> listAppsInWorkspace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    ListAppsResponse response = new ListAppsResponse();
    response.addAll(
        leonardoApiClient.listAppsInProjectCreatedByCreator(dbWorkspace.getGoogleProject()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AppLocalizeResponse> localizeApp(
      String workspaceNamespace,
      String appName,
      Boolean localizeAllFiles,
      AppLocalizeRequest body) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    return ResponseEntity.ok(
        new AppLocalizeResponse()
            .appLocalDirectory(
                interactiveAnalysisService.localize(
                    workspaceNamespace,
                    appName,
                    body.getAppType(),
                    body.getFileNames(),
                    body.isPlaygroundMode(),
                    false,
                    localizeAllFiles)));
  }
}
