import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const title = 'Warning|WARNING';

export default class WarningDiscardChangesModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    return true;
  }

  async clickDiscardChangesButton(): Promise<string[]> {
    const contentText = await this.getTextContent();
    await this.clickButton(LinkText.DiscardChanges, { waitForClose: true });
    return contentText;
  }

  async clickYesDeleteButton(): Promise<string[]> {
    const contentText = await this.getTextContent();
    await this.clickButton(LinkText.YesDelete, { waitForClose: true });
    return contentText;
  }
}
