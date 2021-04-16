import { SideBarLink } from 'app/text-labels';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import BaseHelpSidebar from './base-help-sidebar';
import { logger } from 'libs/logger';

const sidebarTitle = 'Help Tips';

export default class HelpTipsSidebar extends BaseHelpSidebar {
  constructor(page: Page) {
    super(page);
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) return;
    await this.clickIcon(SideBarLink.HelpTips);
    await this.waitUntilVisible();
    await waitForText(this.page, sidebarTitle, { xpath: this.getXpath() });
    // Wait for visible texts
    await this.page.waitForXPath(`${this.getXpath()}//h3`, { visible: true });
    const title = await this.getTitle();
    logger.info(`Opened "${title}" sidebar`);
  }
}
