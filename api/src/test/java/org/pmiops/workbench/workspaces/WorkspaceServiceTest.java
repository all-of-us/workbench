package org.pmiops.workbench.workspaces;

import java.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.db.dao.CohortCloningService;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceServiceTest {

  @Mock
  private CohortCloningService cohortCloningService;
  @Mock
  private ConceptSetService conceptSetService;
  @Mock
  private WorkspaceDao workspaceDao;
  @Mock
  private WorkspaceMapper workspaceMapper;

  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private Clock clock;

  private WorkspaceService workspaceService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService = new WorkspaceServiceImpl(clock, cohortCloningService, conceptSetService,
        fireCloudService, workspaceDao, workspaceMapper);
  }

  @Test
  public void ensureWorkspaceAccessLevelsComparison() {

  }

  @Test
  public void getWorkspacesWithAccessLevel_noAccess() {
    workspaceService.getWorkspacesWithAccessLevel(WorkspaceAccessLevel.NO_ACCESS);
  }

  @Test
  public void getWorkspacesWithAccessLevel_reader() {

  }

  @Test
  public void getWorkspacesWithAccessLevel_writer() {

  }

  @Test
  public void getWorkspacesWithAccessLevel_owner() {

  }
}
