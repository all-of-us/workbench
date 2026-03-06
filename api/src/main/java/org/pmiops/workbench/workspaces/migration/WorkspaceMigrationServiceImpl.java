package org.pmiops.workbench.workspaces.migration;

import jakarta.inject.Provider;
import java.util.Optional;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMigrationServiceImpl implements WorkspaceMigrationService {

  private final WsmClient wsmClient;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final FireCloudService fireCloudService;
  private final InitialCreditsService initialCreditsService;

  @Autowired
  public WorkspaceMigrationServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService) {
    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
  }

  @Override
  public void startWorkspaceMigration(String namespace, String terraName) {

    DbWorkspace dbWorkspace = workspaceDao.getRequired(namespace, terraName);

    dbWorkspace.setMigrationState(MigrationState.STARTING.name());
    workspaceDao.save(dbWorkspace);

    RawlsWorkspaceDetails fcWorkspace =
        fireCloudService.getWorkspace(namespace, terraName).getWorkspace();

    Workspace workspace =
        workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace, initialCreditsService);

    // podId (MATCH EXISTING PATTERN)
    String podId =
        Optional.ofNullable(userDao.findUserByUsername(workspace.getCreator()))
            .map(DbUser::getVwbUserPod)
            .map(DbVwbUserPod::getVwbPodId)
            .orElse(workbenchConfigProvider.get().vwb.defaultPodId);

    wsmClient.createWorkspaceAsService(workspace, podId);

    // TODO: Un-comment this once the Storage
    // Transfer Service (STS) migration flow is finalized.
    //
    //    WorkspaceDescription vwbWorkspace = wsmClient.createWorkspaceAsService(workspace, podId);
    //    String workspaceId = vwbWorkspace.getId().toString();
    //    wsmClient.createControlledBucket(workspaceId, namespace);
  }
}
