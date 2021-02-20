import { Page } from 'puppeteer';
import { LinkText } from 'app/text-labels';
import BaseHelpSidebar from './base-help-sidebar';

export default class ReviewConceptSetSidebar extends BaseHelpSidebar {
  constructor(page: Page) {
    super(page);
  }

  // Not implemented because it's not triggered to open by sidebar tab.
  open(): Promise<void> {
    throw new Error('Do not use. Method not to be implemented.');
  }

  async waitUntilVisible(): Promise<void> {
    await super.waitUntilVisible();
    const title = await this.getTitle();
    console.log(`"${title}" sidebar is opened`);
  }

  async clickSaveConceptSetButton(): Promise<void> {
    await this.clickButton(LinkText.SaveConceptSet);
  }
}
