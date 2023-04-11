import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import CromwellConfigurationPanel from 'app/sidebar/cromwell-configuration-panel';
import BaseElement from 'app/element/base-element';
import { waitForFn } from 'utils/waits-utils';

// Cluster provisioning can take a while, so set a 20 min timeout
jest.setTimeout(20 * 60 * 1000);

describe('Cromwell GKE App', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateCromwellGkeAppTest';

  test('Create and delete a Cromwell GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const appsPanel = new AppsPanel(page);
    await appsPanel.open();

    // Cromwell is not running, so it appears in unexpanded mode

    const unexpandedCromwellXPath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
    const unexpandedCromwell = new Button(page, unexpandedCromwellXPath);

    expect(await unexpandedCromwell.exists()).toBeTruthy();
    await unexpandedCromwell.click();

    // clicking an unexpanded GKE App:
    // 1. closes the apps panel
    // 2. opens the config panel

    const configPanel = new CromwellConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a Cromwell app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="cromwell-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    // clicking create:
    // 1. closes the config panel
    // 2. waits a few seconds
    // 3. opens the apps panel

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

    await appsPanel.isVisible();
    const expandedCromwellXpath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    await page.waitForXPath(expandedCromwellXpath);

    // the Cromwell status should say PROVISIONING

    const cromwellText = await BaseElement.asBaseElement(
      page,
      await page.waitForXPath(expandedCromwellXpath)
    ).getTextContent();
    expect(cromwellText).toContain('Status: PROVISIONING');
    console.log('Cromwell status: PROVISIONING');

    await appsPanel.pollForStatus(expandedCromwellXpath, 'Running', 15 * 60e3);

    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    await appsPanel.pollForStatus(expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const isDeleted = await waitForFn(
      async () => {
        await appsPanel.close();
        await appsPanel.open();
        const unexpanded = new Button(page, unexpandedCromwellXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      2 * 60e3 // with a 2 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });
});
