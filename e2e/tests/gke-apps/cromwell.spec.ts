import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import CromwellConfigurationPanel from 'app/sidebar/cromwell-configuration-panel';
import BaseElement from 'app/element/base-element';
import { waitForFn } from 'utils/waits-utils';

// 10 minutes
jest.setTimeout(10 * 60 * 1000);

describe('Cromwell GKE app', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateCromwellGkeAppTest';

  test('Create and delete a Cromwell GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const applicationsPanel = new AppsPanel(page);
    await applicationsPanel.open();

    // Cromwell is not running, so it appears in unexpanded mode

    const unexpandedCromwellXPath = `${applicationsPanel.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
    const unexpandedCromwell = new Button(page, unexpandedCromwellXPath);

    expect(await unexpandedCromwell.exists()).toBeTruthy();
    await unexpandedCromwell.click();

    // clicking Cromwell expands it, exposing its buttons

    const expandedCromwellXpath = `${applicationsPanel.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    await page.waitForXPath(expandedCromwellXpath);

    const settingsXPath = `${expandedCromwellXpath}//*[@data-test-id="apps-panel-button-Settings"]`;
    const settingsButton = new Button(page, settingsXPath);
    expect(await settingsButton.exists()).toBeTruthy();

    const pauseXPath = `${expandedCromwellXpath}//*[@data-test-id="apps-panel-button-Pause"]`;
    const pauseButton = new Button(page, pauseXPath);
    expect(await pauseButton.exists()).toBeTruthy();

    const launchXPath = `${expandedCromwellXpath}//*[@data-test-id="apps-panel-button-Launch"]`;
    const launchButton = new Button(page, launchXPath);
    expect(await launchButton.exists()).toBeTruthy();

    // clicking the launch button does not launch immediately, but instead:
    // 1. closes the apps panel
    // 2. opens the config panel

    console.log('creating a new Cromwell');

    await launchButton.click();
    await page.waitForXPath(applicationsPanel.getXpath(), { visible: false });
    const configPanel = new CromwellConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a Cromwell app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="cromwell-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    // this closes the config panel, waits a few seconds, and then opens the apps panel again to show status

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

    await applicationsPanel.isVisible();
    await page.waitForXPath(expandedCromwellXpath);

    // the Cromwell status should say PROVISIONING

    const expandedCromwell = BaseElement.asBaseElement(page, await page.waitForXPath(expandedCromwellXpath));
    expect(await expandedCromwell.getTextContent()).toContain('status: PROVISIONING');

    console.log('Cromwell status: PROVISIONING');

    // poll for "Running" by repeatedly closing and opening

    const isRunning = await waitForFn(
      async () => {
        await applicationsPanel.close();
        await applicationsPanel.open();

        return (await expandedCromwell.getTextContent()).includes('status: Running');
      },
      10e3, // every 10 sec
      5 * 60e3 // with a 5 min timeout
    );
    expect(isRunning).toBeTruthy();

    console.log('Cromwell status: Running');

    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    // poll for "DELETING" by repeatedly closing and opening

    const isDeleting = await waitForFn(
      async () => {
        await applicationsPanel.close();
        await applicationsPanel.open();

        return (await expandedCromwell.getTextContent()).includes('status: DELETING');
      },
      10e3, // every 10 sec
      5 * 60e3 // with a 5 min timeout
    );
    expect(isDeleting).toBeTruthy();

    console.log('Cromwell status: DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const isDeleted = await waitForFn(
      async () => {
        await applicationsPanel.close();
        await applicationsPanel.open();
        return await unexpandedCromwell.exists();
      },
      10e3, // every 10 sec
      60e3 // with a 1 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });

  console.log('Cromwell deleted');
});
