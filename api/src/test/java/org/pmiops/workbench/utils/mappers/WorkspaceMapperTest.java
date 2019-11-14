package org.pmiops.workbench.utils.mappers;

import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceMapperTest {

  private Workspace sourceClientWorkspace;
  private DbWorkspace private DbWorkspace sourceDbWorkspace;

  @Autowired private WorkspaceMapper workspaceMapper;
  @Autowired private WorkspaceDao mockWorkspaceDao;

  @TestConfiguration
  @Import({WorkspaceMapperImpl.class})
  @MockBean({WorkspaceDao.class})
  static class Configuration {}


}
