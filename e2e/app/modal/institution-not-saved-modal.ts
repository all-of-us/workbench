import { Page } from 'puppeteer';
import { LinkText } from 'app/text-labels';
import Modal from './modal';
import { waitForText } from 'utils/waits-utils';

const modalTitle = 'Institution not saved';

export default class InstitutionNotSavedModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  // click KeepEditing  button of the Institution Not Saved Modal
  async clickKeepEditingButton(): Promise<void> {
    await this.clickButton(LinkText.KeepEditing);
  }
  // click yes, Leave button of the Institution Not Saved Modal
  async clickYesLeaveButton(): Promise<void> {
    await this.clickButton(LinkText.YesLeave);
  }
}
