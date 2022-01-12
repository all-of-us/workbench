import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

const modalTitle = 'You have confirmed your profile is accurate';

export default class ProfileConfirmationModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  getOKButton(): Button {
    return Button.findByName(this.page, { name: LinkText.OK }, this);
  }
}
