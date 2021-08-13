import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

const modalTitle = 'You have confirmed your profile is accurate';

export default class ProfileConfirmationModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
    return true;
  }

  getOKButton(): Button {
    return Button.findByName(this.page, { name: LinkText.OK }, this);
  }
}
