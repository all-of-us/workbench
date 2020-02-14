package org.pmiops.workbench.workspaceadmin;

import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.TestMockFactory;

public class WorkspaceAdminControllerTest {
  private TestMockFactory testMockFactory;

  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";

  @Before
  public void setUp () {
    testMockFactory = new TestMockFactory();
  }

  @Test
  public void getFederatedWorkspaceDetails() {
    Workspace workspace = testMockFactory.createWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME);
  }
}
