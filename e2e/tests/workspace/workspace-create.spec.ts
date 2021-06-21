import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspacesPage from 'app/page/workspaces-page';
import { makeWorkspaceName } from 'utils/str-utils';
import { config } from 'resources/workbench-config';
import { withPage } from 'libs/page-init';

describe('Creating new workspaces', () => {
  test('Create workspace - NO request for review', async () => {
    const newWorkspaceName = makeWorkspaceName();
    await withPage()(async (page, _browser) => {
      const workspacesPage = new WorkspacesPage(page);
      await workspacesPage.load();

      // create workspace with "No Review Requested" radiobutton selected
      const modalTextContent = await workspacesPage.createWorkspace(newWorkspaceName);

      // Pick out few sentences to verify
      expect(modalTextContent).toContain('Create Workspace');
      expect(modalTextContent).toContain(
        'Will be displayed publicly to inform All of Us research participants. ' +
          'Therefore, please verify that you have provided sufficiently detailed responses in plain language.'
      );
      expect(modalTextContent).toContain('You can also make changes to your answers after you create your workspace.');

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);
      // Verify the CDR version displays in the workspace navigation bar
      expect(await dataPage.getCdrVersion()).toBe(config.defaultCdrVersionName);
      // cleanup
      await dataPage.deleteWorkspace();
    });
  });
});
