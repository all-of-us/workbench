import { findOrCreateWorkspace, findOrCreateWorkspaceCard } from 'utils/test-utils';
import { MenuOption } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';
import { withSignInTest } from 'libs/page-manager';

describe('Duplicate workspace', () => {
  const workspace = 'e2eCloneWorkspaceTest';

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the duplicate.
   * - Delete duplicate workspace.
   */
  test('OWNER can duplicate workspace via Workspace card', async () => {
    await withSignInTest()(async (page) => {
      const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
      await workspaceCard.asElementHandle().hover();
      await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

      // Fill out Workspace Name should be just enough for successful duplication
      const workspaceEditPage = new WorkspaceEditPage(page);
      await workspaceEditPage.getWorkspaceNameTextbox().clear();
      const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

      // observe that we cannot change the Data Access Tier.
      const accessTierSelect = workspaceEditPage.getDataAccessTierSelect();
      expect(await accessTierSelect.isDisabled()).toEqual(true);

      const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
      await workspaceEditPage.requestForReviewRadiobutton(false);
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
      await workspacesPage.waitForLoad();
      expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();
    });
  });

  test('OWNER can access workspace duplicate page via Workspace action menu', async () => {
    await withSignInTest()(async (page) => {
      await findOrCreateWorkspace(page, { workspaceName: workspace });

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

      const workspaceEditPage = new WorkspaceEditPage(page);
      await workspaceEditPage.waitForLoad();

      const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
      expect(await finishButton.isCursorNotAllowed()).toBe(true);

      // Fill out Workspace Name
      await workspaceEditPage.getWorkspaceNameTextbox().clear();
      await workspaceEditPage.fillOutWorkspaceName();
      // select "Share workspace with same set of collaborators radiobutton
      await workspaceEditPage.clickShareWithCollaboratorsCheckbox();
      await workspaceEditPage.requestForReviewRadiobutton(false);
      await finishButton.waitUntilEnabled();
      expect(await finishButton.isCursorNotAllowed()).toBe(false);

      // Click CANCEL button.
      const cancelButton = workspaceEditPage.getCancelButton();
      await cancelButton.clickAndWait();

      await dataPage.waitForLoad();
    });
  });
});
