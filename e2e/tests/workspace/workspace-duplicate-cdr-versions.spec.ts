import { findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { MenuOption } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';

describe('Duplicate workspace, changing CDR versions', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eTestCloneWorkspaceWithOldCDRVersion';

  test('OWNER can duplicate workspace to a newer CDR Version via Workspace card', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName,
      cdrVersion: config.ALTERNATIVE_CDR_VERSION_NAME
    });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.DEFAULT_CDR_VERSION_NAME);

    const upgradeMessage = await workspaceEditPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspaceName);
    expect(upgradeMessage).toContain(`${config.ALTERNATIVE_CDR_VERSION_NAME} to ${config.DEFAULT_CDR_VERSION_NAME}.`);

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

    // Verify Delete action was successful.
    expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();
  });
});
