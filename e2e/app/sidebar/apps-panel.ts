import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import { waitForFn } from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';
import Button from 'app/element/button';
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

  async deleteCromwellGkeApp(): Promise<boolean> {
    await this.open();

    const expandedCromwellXpath = `${this.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();
    const warningDeleteCromwellModal = new WarningDeleteCromwellModal(page);
    expect(warningDeleteCromwellModal.isLoaded());
    await warningDeleteCromwellModal.clickYesDeleteButton();

    await this.pollForStatus(expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const unexpandedCromwellXPath = `${this.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
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
