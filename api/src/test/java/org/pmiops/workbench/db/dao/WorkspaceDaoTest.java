package org.pmiops.workbench.db.dao;

import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceDaoTest {

  private static final String WORKSPACE_1_NAME = "Foo";
  private static final String WORKSPACE_NAMESPACE = "aou-1";

  @Autowired private WorkspaceDao workspaceDao;

  public DbWorkspace createWorkspace() {
    DbWorkspace workspace1 = new DbWorkspace();
    workspace1.setName(WORKSPACE_1_NAME);
    workspace1.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace1 = workspaceDao.save(workspace1);
    return workspace1;
  }
}
