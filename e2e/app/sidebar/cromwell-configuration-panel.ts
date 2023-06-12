import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import Button from '../element/button';
import AppsPanel from './apps-panel';

const defaultXpath = '//*[@id="cromwell-configuration-panel"]';

export default class CromwellConfigurationPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.CromwellConfiguration);
  }

  async startCromwellGkeApp(): Promise<void> {
    // Cromwell is not running, so it appears in unexpanded mode
    const appsPanel = new AppsPanel(page);
    await appsPanel.clickUnexpandedApp('Cromwell');

    const configPanel = new CromwellConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a Cromwell app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="Cromwell-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    return;
  }
}
