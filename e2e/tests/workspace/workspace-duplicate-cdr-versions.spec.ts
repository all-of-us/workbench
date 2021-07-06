import { findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { MenuOption } from 'app/text-labels';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';

describe('Duplicate workspace, changing CDR versions', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const originalWorkspaceName1 = 'e2eCloneWorkspaceWithOldCDRVersion1';

  test('OWNER can duplicate workspace to an older CDR Version after consenting to restrictions', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: originalWorkspaceName1 });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.ALTERNATIVE_CDR_VERSION);

    // wait for the warning modal and consent to the required restrictions
    const modal = new OldCdrVersionModal(page);
    await modal.consentToOldCdrRestrictions();

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

  const originalWorkspaceName2 = 'e2eCloneWorkspaceWithOldCDRVersion2';

  test('OWNER can duplicate workspace to a newer CDR Version via Workspace card', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName: originalWorkspaceName2,
      cdrVersion: config.ALTERNATIVE_CDR_VERSION
    });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.DEFAULT_CDR_VERSION);

    const upgradeMessage = await workspaceEditPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(originalWorkspaceName2);
    expect(upgradeMessage).toContain(`${config.ALTERNATIVE_CDR_VERSION} to ${config.DEFAULT_CDR_VERSION}.`);

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
