import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

const modalTitle = "You have selected the Controlled Tier but you don't have access";

export default class UnavailableTierModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    const xpath = '//*[@data-test-id="unavailable-tier-modal"]';
    await this.page.waitForXPath(xpath, { visible: true });
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  getGetStartedButton(): Button {
    return Button.findByName(this.page, { name: LinkText.GetStarted });
  }
}
