import { Page } from 'puppeteer';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceBase from 'app/page/workspace-base';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import CdrVersionUpgradeModal from 'app/modal/cdr-version-upgrade-modal';

describe('Workspace CDR Version Upgrade modal', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eUpgradeWorkspaceCDRVersionTest';

  test('Clicking Cancel and Upgrade buttons', async () => {
    await findOrCreateWorkspace(page, { cdrVersion: config.altCdrVersionName, workspaceName: workspace });

    const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);
    const cdrVersion = await workspacePage.getCdrVersion();
    expect(cdrVersion).toBe(config.altCdrVersionName);

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
    const upgradeMessage = await duplicationPage.getCdrVersionUpgradeMessage();
    expect(upgradeMessage).toContain(workspace);
    expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

    const editCancelButton = duplicationPage.getCancelButton();
    await editCancelButton.clickAndWait();
    expect(await workspacePage.isLoaded()).toBe(true);
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
