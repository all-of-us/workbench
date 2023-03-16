import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import CromwellConfigurationPanel from 'app/sidebar/cromwell-configuration-panel';
import BaseElement from 'app/element/base-element';
import { waitForFn } from 'utils/waits-utils';
import { Page } from 'puppeteer';

// 10 minutes
jest.setTimeout(10 * 60 * 1000);

// TODO is this already a thing?
const getTextFromXPath = async (page: Page, xPath: string): Promise<string> =>
  BaseElement.asBaseElement(page, await page.waitForXPath(xPath)).getTextContent();

const pollForStatus = async (page: Page, appsPanel: AppsPanel, expandedCromwellXpath: string, status: string) => {
  const success = await waitForFn(
    async () => {
      await appsPanel.close();
      await appsPanel.open();

      return (await getTextFromXPath(page, expandedCromwellXpath)).includes(`status: ${status}`);
    },
    10e3, // every 10 sec
    10 * 60e3 // with a 10 min timeout
  );
  expect(success).toBeTruthy();

  console.log(`Cromwell status: ${status}`);
};

describe('Cromwell GKE App', () => {
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
    // 2. opens the apps panel

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

    await applicationsPanel.isVisible();
    const expandedCromwellXpath = `${applicationsPanel.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    await page.waitForXPath(expandedCromwellXpath);

    // the Cromwell status should say PROVISIONING

    expect(await getTextFromXPath(page, expandedCromwellXpath)).toContain('status: PROVISIONING');
    console.log('Cromwell status: PROVISIONING');

    // poll for "Running" by repeatedly closing and opening
    await pollForStatus(page, applicationsPanel, expandedCromwellXpath, 'Running');

    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    // poll for "DELETING" by repeatedly closing and opening
    await pollForStatus(page, applicationsPanel, expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const isDeleted = await waitForFn(
      async () => {
        await applicationsPanel.close();
        await applicationsPanel.open();
        const unexpanded = new Button(page, unexpandedCromwellXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      60e3 // with a 1 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });

  console.log('Cromwell deleted');
});
