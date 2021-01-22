import {findOrCreateWorkspace, signInWithAccessToken} from 'utils/test-utils';
import {MenuOption} from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, {NavLink} from 'app/component/navigation';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';

describe('Duplicate workspace', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the duplicate.
   * - Delete duplicate workspace.
   */
  test('OWNER can duplicate workspace via Workspace card', async () => {
      const workspaceCard = await findOrCreateWorkspace(page);

      await workspaceCard.asElementHandle().hover();
      // Click on Ellipsis menu "Duplicate" option.
      await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, {waitForNav: true});

      // Fill out Workspace Name should be just enough for successful duplication
      const workspaceEditPage = new WorkspaceEditPage(page);
      await (await workspaceEditPage.getWorkspaceNameTextbox()).clear();
      const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

      const finishButton = await workspaceEditPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspaceEditPage.clickCreateFinishButton(finishButton);

      // Duplicate workspace Data page is loaded.
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();
      expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

      // Delete duplicate workspace via Workspace card in Your Workspaces page.
      await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
      const workspacesPage = new WorkspacesPage(page);
      await workspacesPage.waitForLoad();

      await WorkspaceCard.deleteWorkspace(page, duplicateWorkspaceName);

      // Verify Delete action was successful.
      expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();
    });

  test('OWNER can duplicate workspace via Workspace action menu', async () => {
      const workspaceCard = await findOrCreateWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

      const workspaceEditPage = new WorkspaceEditPage(page);

      // Fill out Workspace Name
      await (await workspaceEditPage.getWorkspaceNameTextbox()).clear();
      const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();
      // select "Share workspace with same set of collaborators radiobutton
      await workspaceEditPage.clickShareWithCollaboratorsCheckbox();

      const finishButton = await workspaceEditPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspaceEditPage.clickCreateFinishButton(finishButton);

      // Duplicate workspace Data page is loaded.
      await dataPage.waitForLoad();
      expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

      // Delete duplicate workspace via Workspace action dropdown menu.
      await dataPage.deleteWorkspace();
    });
});
