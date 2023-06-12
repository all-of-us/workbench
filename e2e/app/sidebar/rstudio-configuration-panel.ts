import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import Button from '../element/button';
import AppsPanel from './apps-panel';

const defaultXpath = '//*[@id="rstudio-configuration-panel"]';

export default class RStudioConfigurationPanel extends BaseEnvironmentPanel {
  constructor(page: Page) {
    super(page, defaultXpath, SideBarLink.RStudioConfiguration);
  }

  async startRStudioGkeApp(): Promise<void> {
    // RStudio is not running, so it appears in unexpanded mode
    const appsPanel = new AppsPanel(page);
    await appsPanel.clickUnexpandedApp('RStudio');

    const configPanel = new RStudioConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a RStudio app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="RStudio-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    return;
  }
}
