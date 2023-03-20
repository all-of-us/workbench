import BaseSidebar from './base-sidebar';
import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import { logger } from 'libs/logger';

// for logic common to environment-related panels (user apps and runtimes)
export default class BaseEnvironmentPanel extends BaseSidebar {
  panelIcon: SideBarLink;

  constructor(page: Page, xpath: string, panelIcon: SideBarLink) {
    super(page, xpath);
    this.panelIcon = panelIcon;
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) {
      return;
    }
    await this.clickIcon(this.panelIcon);

    await this.page.waitForXPath(`${this.getXpath()}//h3`, { visible: true });
    logger.info(`Opened "${await this.getTitle()}" sidebar panel`);
  }
}
