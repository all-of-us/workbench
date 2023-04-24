import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import { waitForFn } from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';
import Button from '../element/button';
import expect from 'expect';
import CromwellConfigurationPanel from './cromwell-configuration-panel';
import WarningDeleteCromwellModal from '../modal/warning-delete-cromwell-modal';

const defaultXpath = '//*[@data-test-id="apps-panel"]';

export default class AppsPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.UserApps);
  }

  async pollForStatus(xPath: string, status: string, timeout: number = 10 * 60e3): Promise<void> {
    const interval = 10e3; // every 10 sec
    const success = await waitForFn(
      async () => {
        const text = await BaseElement.asBaseElement(page, await page.waitForXPath(xPath)).getTextContent();
        return text.includes(`Status: ${status}`);
      },
      interval,
      timeout
    );
    console.log(success ? `Polling complete, status = ${status}` : `Polling timed out after ${interval / 1e3} seconds`);
    expect(success).toBeTruthy();
  }

  async startGkeApp(appName: string): Promise<void> {
    console.log('start');
    const appsPanel = new AppsPanel(page);
    await appsPanel.open();

    const unexpandedCromwellXPath = `${appsPanel.getXpath()}//*[@data-test-id="${appName}-unexpanded"]`;
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
    await appsPanel.close();
    return;
  }

  async deleteGkeApp(appName: string): Promise<boolean> {
    await this.open();

    const expandedCromwellXpath = `${this.getXpath()}//*[@data-test-id="${appName}-expanded"]`;
    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="${appName}-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    await this.pollForStatus(expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const unexpandedCromwellXPath = `${this.getXpath()}//*[@data-test-id="${appName}-unexpanded"]`;

    const warningDeleteCromwellModal = new WarningDeleteCromwellModal(page);
    expect(warningDeleteCromwellModal.isLoaded());
    await warningDeleteCromwellModal.clickYesDeleteButton();
    const isDeleted = await waitForFn(
      async () => {
        await this.close();
        await this.open();
        const unexpanded = new Button(page, unexpandedCromwellXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      2 * 60e3 // with a 2 min timeout
    );
    return isDeleted;
  }
}
