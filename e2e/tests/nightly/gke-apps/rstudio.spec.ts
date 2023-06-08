import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import { waitForFn } from 'utils/waits-utils';
import ConfirmDeleteEnvironmentWithPdPanel from 'app/sidebar/confirm-delete-environment-with-pd-panel';
import { SideBarLink } from 'app/text-labels';
import RStudioConfigurationPanel from 'app/sidebar/rstudio-configuration-panel';

// Cluster provisioning can take a while, so set a 20 min timeout
jest.setTimeout(20 * 60 * 1000);

describe('RStudio GKE App', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRStudioGkeAppTest';

  test('Create and delete a RStudio GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const configPanel = new RStudioConfigurationPanel(page);
    await configPanel.startRStudioGkeApp();

    // 1. closes the config panel
    // 2. waits a few seconds
    // 3. opens the apps panel

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

    const appsPanel = new AppsPanel(page);
    await appsPanel.isVisible();

    const expandedRStudioXpath = `${appsPanel.getXpath()}//*[@data-test-id="RStudio-expanded"]`;
    await page.waitForXPath(expandedRStudioXpath);

    await appsPanel.pollForStatus(expandedRStudioXpath, 'PROVISIONING');

    await appsPanel.pollForStatus(expandedRStudioXpath, 'Running', 15 * 60e3);

    const deleteXPath = `${expandedRStudioXpath}//*[@data-test-id="RStudio-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    const confirmDeleteEnvironmentWithPdPanel = new ConfirmDeleteEnvironmentWithPdPanel(
      page,
      SideBarLink.RStudioConfiguration
    );
    await confirmDeleteEnvironmentWithPdPanel.confirmDeleteGkeAppWithDisk();

    await appsPanel.open();
    await appsPanel.pollForStatus(expandedRStudioXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const unexpandedRStudioXPath = `${appsPanel.getXpath()}//*[@data-test-id="RStudio-unexpanded"]`;
    const isDeleted = await waitForFn(
      async () => {
        await appsPanel.close();
        await appsPanel.open();
        const unexpanded = new Button(page, unexpandedRStudioXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      2 * 60e3 // with a 2 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });
});
