import { Page } from 'puppeteer';
import { findOrCreateWorkspace, findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceBase from 'app/page/workspace-base';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import CdrVersionUpgradeModal from 'app/modal/cdr-version-upgrade-modal';
import { MenuOption } from 'app/text-labels';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/card/workspace-card';

describe.skip('Workspace CDR Version Upgrade modal', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eWorkspaceUpgradeCDRVersionTest';

  test('CDR Upgrade model', async () => {
    await findOrCreateWorkspace(page, { cdrVersion: config.OLD_CDR_VERSION_NAME, workspaceName });

    const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);
    const cdrVersion = await workspacePage.getCdrVersion();
    expect(cdrVersion).toBe(config.OLD_CDR_VERSION_NAME);

    let modal = await launchCdrUpgradeModal(page);

    // Clicking the Cancel
    const modalCancelButton = modal.getCancelButton();
    await modalCancelButton.click();

    // CDR version flag remains
    await workspacePage.getNewCdrVersionFlag();

    // Clicking the Upgrade button opens the Duplicate Workspace Page
    modal = await launchCdrUpgradeModal(page);
    const upgradeButton = modal.getUpgradeButton();
    await upgradeButton.click();

    const duplicationPage = new WorkspaceEditPage(page);
    await duplicationPage.waitForLoad();
    const upgradeMessage = await duplicationPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspaceName);
    expect(upgradeMessage).toContain(`${config.OLD_CDR_VERSION_NAME} to ${config.DEFAULT_CDR_VERSION_NAME}.`);

    const editCancelButton = duplicationPage.getCancelButton();
    await editCancelButton.clickAndWait();
    expect(await workspacePage.isLoaded()).toBe(true);
  });

  test('Duplicate Workspace with older CDR version to newer CDR version', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName,
      cdrVersion: config.OLD_CDR_VERSION_NAME
    });
    await (await workspaceCard.asElement()).hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.DEFAULT_CDR_VERSION_NAME);

    const upgradeMessage = await workspaceEditPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspaceName);
    expect(upgradeMessage).toContain(`${config.OLD_CDR_VERSION_NAME} to ${config.DEFAULT_CDR_VERSION_NAME}.`);

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

    await new WorkspaceCard(page).delete({ name: duplicateWorkspaceName });
  });
});

async function launchCdrUpgradeModal(page: Page): Promise<CdrVersionUpgradeModal> {
  const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);

  // Clicking the CDR version upgrade flag pops up the upgrade modal
  const newVersionFlag = await workspacePage.getNewCdrVersionFlag();
  await newVersionFlag.click();

  const modal = new CdrVersionUpgradeModal(page);
  expect(await modal.isLoaded()).toBe(true);
  return modal;
}
