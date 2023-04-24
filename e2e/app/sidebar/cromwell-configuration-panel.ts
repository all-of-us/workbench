import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import Button from '../element/button';
import expect from 'expect';
import AppsPanel from './apps-panel';
import WarningDeleteCromwellModal from '../modal/warning-delete-cromwell-modal';
import { waitForFn } from 'utils/waits-utils';

const defaultXpath = '//*[@id="cromwell-configuration-panel"]';

export default class CromwellConfigurationPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.CromwellConfiguration);
  }

  async startCromwellGkeApp(): Promise<void> {
    // Cromwell is not running, so it appears in unexpanded mode
    const appsPanel = new AppsPanel(page);
    await appsPanel.open();
    const unexpandedCromwellXPath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
    const unexpandedCromwell = new Button(page, unexpandedCromwellXPath);

    expect(await unexpandedCromwell.exists()).toBeTruthy();
    await unexpandedCromwell.click();

    const configPanel = new CromwellConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a Cromwell app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="Cromwell-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    await appsPanel.close();
    return;
  }

  async deleteCromwellGkeApp(): Promise<boolean> {
    const appsPanel = new AppsPanel(page);
    await appsPanel.open();

    const expandedCromwellXpath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();
    const warningDeleteCromwellModal = new WarningDeleteCromwellModal(page);
    expect(warningDeleteCromwellModal.isLoaded());
    await warningDeleteCromwellModal.clickYesDeleteButton();

    await appsPanel.pollForStatus(expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const unexpandedCromwellXPath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
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
    return isDeleted;
  }
}
