import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import { waitForFn } from 'utils/waits-utils';

// Cluster provisioning can take a while, so set a 20 min timeout
jest.setTimeout(20 * 60 * 1000);

describe('RStudio GKE App', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRStudioGkeAppTest';

  test('Create and delete a RStudio GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const appsPanel = new AppsPanel(page);
    await appsPanel.open();

    // RStudio is not running, so it appears in unexpanded mode

    const unexpandedRStudioXPath = `${appsPanel.getXpath()}//*[@data-test-id="RStudio-unexpanded"]`;
    const unexpandedRStudio = new Button(page, unexpandedRStudioXPath);

    expect(await unexpandedRStudio.exists()).toBeTruthy();
    await unexpandedRStudio.click();

    // clicking RStudio expands it, exposing its buttons

    const expandedRStudioXpath = `${appsPanel.getXpath()}//*[@data-test-id="RStudio-expanded"]`;
    await page.waitForXPath(expandedRStudioXpath);

    const createXPath = `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Create"]`;
    const createButton = new Button(page, createXPath);
    expect(await new Button(page, createXPath).exists()).toBeTruthy();

    const pauseXPath = `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Pause"]`;
    expect(await new Button(page, pauseXPath).exists()).toBeTruthy();

    const launchXPath = `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Launch"]`;
    expect(await new Button(page, launchXPath).exists()).toBeTruthy();

    await createButton.click();

    // poll for "PROVISIONING" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'PROVISIONING');

    // poll for "Running" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'Running', 15 * 60e3);

    const pauseButton = new Button(page, pauseXPath);
    await pauseButton.click();

    // poll for "Pausing" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'Pausing', 15 * 60e3);

    // poll for "Paused" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'Paused', 15 * 60e3);

    const resumeXPath = `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Resume"]`;
    const resumeButton = new Button(page, resumeXPath);
    await resumeButton.click();

    // poll for "Resuming" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'Resuming', 15 * 60e3);

    // poll for "Running" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'Running', 15 * 60e3);

    const deleteXPath = `${expandedRStudioXpath}//*[@data-test-id="RStudio-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    // poll for "DELETING" by repeatedly closing and opening
    await appsPanel.pollForStatus(expandedRStudioXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const isDeleted = await waitForFn(
      async () => {
        await appsPanel.close();
        await appsPanel.open();
        const unexpanded = new Button(page, unexpandedRStudioXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      60e3 // with a 1 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });
});
