import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const title = 'Warning!';

export default class WarningDiscardChangesModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { xpath: this.getXpath() });
    return true;
  }

  async clickDiscardChangesButton(): Promise<string[]> {
    const contentText = await this.getTextContent();
    await this.clickButton(LinkText.DiscardChanges, { waitForNav: true, waitForClose: true });
    await waitWhileLoading(this.page);
    return contentText;
  }
}
