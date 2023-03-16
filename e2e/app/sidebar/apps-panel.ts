import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseSidebar from './base-sidebar';
import { logger } from 'libs/logger';

const defaultXpath = '//*[@data-test-id="apps-panel"]';

export default class AppsPanel extends BaseSidebar {
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
    await this.clickIcon(SideBarLink.UserApps);

    await this.page.waitForXPath(`${this.getXpath()}//h3`, { visible: true });
    logger.info(`Opened "${await this.getTitle()}" sidebar`);
  }
}
