import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import ApplicationsPanel from 'app/sidebar/applications-panel';
import Button from 'app/element/button';
import BaseElement from 'app/element/base-element';
import { waitForAsyncFn } from 'utils/waits-utils';

// 10 minutes
jest.setTimeout(10 * 60 * 1000);

describe('RStudio GKE app', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRStudioGkeAppTest';

  test('Users can create, launch, and delete an RStudio GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    // Create an RStudio environment
    const applicationsPanel = new ApplicationsPanel(page);
    await applicationsPanel.open();
    const unexpandedRStudioXpath = `${applicationsPanel.getXpath()}//*[@data-test-id="RStudio-unexpanded"]`;
    const unexpandedRStudio = new Button(page, unexpandedRStudioXpath);
    await unexpandedRStudio.click();
    const expandedRStudioXpath = `${applicationsPanel.getXpath()}//*[@data-test-id="RStudio-expanded"]`;
    const expandedRStudio = new BaseElement(page, expandedRStudioXpath);
    await new Button(page, `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Create"]`).click();
    await page.reload();
    expect(await expandedRStudio.getTextContent()).toContain('status: PROVISIONING');
    const isRunning = await waitForAsyncFn(
      async () => {
        await page.reload();
        return (await expandedRStudio.getTextContent()).includes('status: Running');
      },
      10e3, // every 10 sec
      5 * 60e3 // with a 5 min timeout
    );
    expect(isRunning).toBeTruthy();

    // Launch RStudio
    // This code is hacky, but we expect it to significantly change when we implement RW-9664
    await new Button(page, `${expandedRStudioXpath}//*[@data-test-id="apps-panel-button-Launch"]`).click();
    await new Promise((res) => setTimeout(res, 15 * 1000));
    const pages = await browser.pages();
    const rstudioTab = pages[2];
    const rstudioTabTitle = await rstudioTab.title();
    expect(rstudioTabTitle).toEqual('RStudio Server');
    await rstudioTab.close();

    // Delete RStudio
    await new Button(page, `${expandedRStudioXpath}//*[@data-test-id="RStudio-delete-button"]`).click();
    await page.reload();
    expect(await expandedRStudio.getTextContent()).toContain('status: DELETING');
    const isDeleted = await waitForAsyncFn(
      async () => {
        await page.reload();
        // The apps panel displays before all data has loaded. Wait a few seconds before checking for RStudio.
        await new Promise((res) => setTimeout(res, 10 * 1000));
        return !(await expandedRStudio.exists()) && (await unexpandedRStudio.exists());
      },
      10e3, // every 10 sec
      3 * 60e3 // with a 3 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });
});
