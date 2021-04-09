import { Page } from 'puppeteer';
import { LinkText } from 'app/text-labels';
import BaseHelpSidebar from './base-help-sidebar';
import { logger } from 'libs/logger';

export default class ReviewConceptSetSidebar extends BaseHelpSidebar {
  constructor(page: Page) {
    super(page);
  }

  // Not implemented because it's not triggered to open by sidebar tab.
  // eslint-disable-next-line @typescript-eslint/require-await
  async open(): Promise<void> {
    throw new Error('Do not use. Method not to be implemented.');
  }

  async waitUntilVisible(): Promise<void> {
    await super.waitUntilVisible();
    const title = await this.getTitle();
    logger.info(`"${title}" sidebar is opened`);
  }

  async clickSaveConceptSetButton(): Promise<void> {
    await this.clickButton(LinkText.SaveConceptSet);
  }
}
