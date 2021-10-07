import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const title = 'Warning|WARNING';

export default class WarningDiscardChangesModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    return true;
  }

  async clickDiscardChangesButton(): Promise<string[]> {
    const contentText = await this.getTextContent();
    await this.clickButton(LinkText.DiscardChanges, { waitForNav: true, waitForClose: true });
    return contentText;
  }

  async clickYesDeleteButton(): Promise<string[]> {
    const contentText = await this.getTextContent();
    await this.clickButton(LinkText.YesDelete, { waitForNav: false, waitForClose: true });
    return contentText;
  }
}
