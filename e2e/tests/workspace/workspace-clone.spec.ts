import WorkspacesPage from 'app/page/workspaces-page';
import {findWorkspace, signIn} from 'utils/test-utils';
import {EllipsisMenuAction} from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, {NavLink} from 'app/component/navigation';
import WorkspaceCard from 'app/component/workspace-card';

describe('Clone workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the clone.
   * - Delete clone workspace.
   */
    test('OWNER can clone workspace via Workspace card', async () => {
      const workspaceCard = await findWorkspace(page);

      await workspaceCard.asElementHandle().hover();
      // Click on Ellipsis menu "Duplicate" option.
      await workspaceCard.clickEllipsisAction(EllipsisMenuAction.Duplicate);

      // Fill out Workspace Name should be just enough for clone successfully
      const workspacesPage = new WorkspacesPage(page);
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspacesPage.clickCreateFinishButton(finishButton);

      // Clone workspace Data page is loaded.
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();
      expect(page.url()).toContain(cloneWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

      // Delete clone workspace via Workspace card in Your Workspaces page.
      await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
      await WorkspaceCard.deleteWorkspace(page, cloneWorkspaceName);

      // Verify Delete action was successful.
      expect(await WorkspaceCard.findCard(page, cloneWorkspaceName)).toBeFalsy();
    });

    test('OWNER can clone workspace via Workspace action menu', async () => {
      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.selectWorkspaceAction(EllipsisMenuAction.Duplicate);

      const workspacesPage = new WorkspacesPage(page);

      // Fill out Workspace Name
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const cloneWorkspaceName = await workspacesPage.fillOutWorkspaceName();
      // select "Share workspace with same set of collaborators radiobutton
      await workspacesPage.clickShareWithCollaboratorsCheckbox();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspacesPage.clickCreateFinishButton(finishButton);

      // Clone workspace Data page is loaded.
      await dataPage.waitForLoad();
      expect(page.url()).toContain(cloneWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

      // Delete clone workspace via Workspace action dropdown menu.
      await dataPage.deleteWorkspace();
    });

});
