import BaseSidebar from './base-sidebar';
import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import { logger } from 'libs/logger';

const defaultXpath = '//*[@id="cromwell-configuration-panel"]';

export default class CromwellConfigurationPanel extends BaseSidebar {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page);
    super.setXpath(`${super.getXpath()}${xpath}`);
  }

  // todo: combine with other sidebar panels?
  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) {
      return;
    }
    await this.clickIcon(SideBarLink.CromwellConfiguration);
    // await this.getDeleteIcon();
    // const timeoutForDeletion = 3 * 60 * 1000;
    // await this.waitUntilVisible(timeoutForDeletion);
    // Wait for visible texts
    await this.page.waitForXPath(`${this.getXpath()}//h3`, { visible: true });
    logger.info(`Opened "${await this.getTitle()}" sidebar`);
  }
}
