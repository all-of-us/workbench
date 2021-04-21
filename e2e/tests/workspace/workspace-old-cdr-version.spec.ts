import { Page } from 'puppeteer';
import { findOrCreateWorkspace, findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceBase from 'app/page/workspace-base';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import CdrVersionUpgradeModal from 'app/modal/cdr-version-upgrade-modal';
import { MenuOption } from 'app/text-labels';
import { logger } from 'libs/logger';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';

describe('Workspace CDR Version Upgrade modal', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Workspace name with an older CDR version
  const workspace = 'e2eWorkspaceOldCDRVersionTest';

  test('Clicking Cancel and Upgrade buttons', async () => {
    logger.info('Running test name: Clicking Cancel and Upgrade buttons');
    // Create workspace with an old CDR Version after consenting to restrictions
    await findOrCreateWorkspace(page, {
      workspaceName: workspace,
      cdrVersion: config.altCdrVersionName
    });

    // Verify CDR version in Data page
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    const cdrVersion = await dataPage.getCdrVersion();
    expect(cdrVersion).toBe(config.altCdrVersionName);

    let modal = await launchCdrUpgradeModal(page);

    // Clicking the Cancel
    const modalCancelButton = modal.getCancelButton();
    await modalCancelButton.click();

    // new CDR version flag remains
    const cdrVersionFlag = await dataPage.getNewCdrVersionFlag();
    expect(await cdrVersionFlag.waitForXPath()).toBeTruthy();

    // Clicking the Upgrade button to open the Duplicate Workspace Page
    modal = await launchCdrUpgradeModal(page);
    const upgradeButton = modal.getUpgradeButton();
    await upgradeButton.click();

    const duplicationPage = new WorkspaceEditPage(page);
    await duplicationPage.waitForLoad();
    const upgradeMessage = await duplicationPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspace);
    expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

    const editCancelButton = duplicationPage.getCancelButton();
    await editCancelButton.clickAndWait();
    expect(await dataPage.isLoaded()).toBe(true);
  });

  test('Cannot edit workspace to a newer CDR version', async () => {
    logger.info('Running test name: Cannot edit workspace to a newer CDR version');
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName: workspace,
      cdrVersion: config.altCdrVersionName
    });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: true });

    // CDR Version Select is readonly and diplays old CDR version
    const workspaceEditPage = new WorkspaceEditPage(page);
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    const cdrVersion = await cdrVersionSelect.getSelectedValue();
    expect(cdrVersion).toEqual(config.altCdrVersionName);

    const isSelectReadOnly = await cdrVersionSelect.isDisabled();
    expect(isSelectReadOnly).toBe(true);

    // The Update button is clickable
    const updateButton = workspaceEditPage.getUpdateWorkspaceButton();
    const isButtonReadOnly = await updateButton.isCursorNotAllowed();
    expect(isButtonReadOnly).toBe(false);

    // Update Workspace is successful without making any changes
    await workspaceEditPage.clickCreateFinishButton(updateButton);
    await new WorkspaceDataPage(page).waitForLoad();
  });

  test('Can duplicate workspace to a newer CDR version', async () => {
    logger.info('Running test name: Can duplicate workspace to a newer CDR version');
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName: workspace,
      cdrVersion: config.altCdrVersionName
    });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const cloneWorkspace = await workspaceEditPage.fillOutWorkspaceName();

    // Change to new CDR version
    await workspaceEditPage.selectCdrVersion(config.defaultCdrVersionName);

    const upgradeMessage = await workspaceEditPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspace);
    expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

    await workspaceEditPage.requestForReviewRadiobutton(false);
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await workspaceEditPage.clickCreateFinishButton(duplicateButton);

    // Verify clone workspace was successful
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    await dataPage.verifyWorkspaceNameOnDataPage(cloneWorkspace);

    const cdrVersion = await dataPage.getCdrVersion();
    expect(cdrVersion).toBe(config.defaultCdrVersionName);

    await dataPage.deleteWorkspace();
  });

  test('Can duplicate workspace with same old CDR version', async () => {
    logger.info('Running test name: Can duplicate workspace with same old CDR version');
    const workspaceCard = await findOrCreateWorkspaceCard(page, {
      workspaceName: workspace,
      cdrVersion: config.altCdrVersionName
    });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const cloneWorkspace = await workspaceEditPage.fillOutWorkspaceName();

    // Default is new CDR version. Change to an older CDR version
    const cdrSelect = workspaceEditPage.getCdrVersionSelect();
    expect(await cdrSelect.getSelectedValue()).toBe(config.defaultCdrVersionName);

    await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);
    // Wait for the warning modal and consent to the required restrictions
    const modal = new OldCdrVersionModal(page);
    await modal.consentToOldCdrRestrictions();

    await workspaceEditPage.requestForReviewRadiobutton(false);
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await workspaceEditPage.clickCreateFinishButton(duplicateButton);

    // Verify clone workspace was successful
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    await dataPage.verifyWorkspaceNameOnDataPage(cloneWorkspace);

    const cdrVersion = await dataPage.getCdrVersion();
    expect(cdrVersion).toBe(config.altCdrVersionName);

    await dataPage.deleteWorkspace();
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
