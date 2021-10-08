import { Page } from 'puppeteer';
import { LinkText } from 'app/text-labels';
import Modal from './modal';
import { waitForText } from 'utils/waits-utils';

const modalTitle = 'Institution not saved';

export default class InstitutionNotSavedModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
<<<<<<< HEAD
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
=======
    await waitForText(this.page, modalTitle, { container: this });
>>>>>>> master
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
