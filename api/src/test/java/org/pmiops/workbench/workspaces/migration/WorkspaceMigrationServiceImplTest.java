package org.pmiops.workbench.workspaces.migration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMigrationServiceImplTest {

  private static final String NAMESPACE = "test-ns";
  private static final String TERRA_NAME = "test-ws";
  private static final String POD_ID = "pod-123";

  @Mock private WsmClient wsmClient;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private WorkspaceMapper workspaceMapper;
  @Mock private UserDao userDao;
  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private FireCloudService fireCloudService;
  @Mock private InitialCreditsService initialCreditsService;

  @InjectMocks private WorkspaceMigrationServiceImpl service;

  private DbWorkspace dbWorkspace;
  private Workspace workspace;

  @BeforeEach
  void setup() {
    dbWorkspace = new DbWorkspace();

    dbWorkspace.setWorkspaceNamespace(NAMESPACE);
    dbWorkspace.setFirecloudName(TERRA_NAME);

    workspace = new Workspace();
    workspace.setNamespace(NAMESPACE);
    workspace.setName(TERRA_NAME);
    workspace.setCreator("user@test.com");

    RawlsWorkspaceDetails rawlsWorkspace = new RawlsWorkspaceDetails();

    when(fireCloudService.getWorkspace(NAMESPACE, TERRA_NAME))
        .thenReturn(new RawlsWorkspaceResponse().workspace(rawlsWorkspace));

    when(workspaceMapper.toApiWorkspace(
            eq(dbWorkspace), any(RawlsWorkspaceDetails.class), eq(initialCreditsService)))
        .thenReturn(workspace);

    DbUser dbUser = new DbUser();
    DbVwbUserPod pod = new DbVwbUserPod();
    pod.setVwbPodId(POD_ID);
    dbUser.setVwbUserPod(pod);

    when(userDao.findUserByUsername(any())).thenReturn(dbUser);

    WorkbenchConfig config = new WorkbenchConfig();
    config.vwb = new WorkbenchConfig.VwbConfig();
    config.vwb.defaultPodId = "default-pod";
    when(workbenchConfigProvider.get()).thenReturn(config);

    when(workspaceDao.getRequired(NAMESPACE, TERRA_NAME)).thenReturn(dbWorkspace);
  }

  @Test
  void startWorkspaceMigration_success() {
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME);

    verify(workspaceDao).getRequired(NAMESPACE, TERRA_NAME);

    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.STARTING.name().equals(ws.getMigrationState())));

    assertThat(dbWorkspace.getMigrationState()).isEqualTo(MigrationState.STARTING.name());

    verify(wsmClient).createWorkspaceAsService(workspace, POD_ID);
  }
}
