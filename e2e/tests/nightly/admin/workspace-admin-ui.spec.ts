import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import WorkspaceAdminPage from 'app/page/admin-workspace-page';

describe('Workspace Admin', () => {
    const WorkspaceName = 'Admin testing';
  
    beforeEach(async () => {
      await signInWithAccessToken(page, config.ADMIN_TEST_ACCESS_TOKEN_FILE);
      await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    });
  
    test.only('check the Workspace Admin page UI', async () => {
      const workspaceAdminPage = new WorkspaceAdminPage(page);
      await workspaceAdminPage.waitForLoad();
      workspaceAdminPage.getLoadWorkspaceButton();
     
    });
    });