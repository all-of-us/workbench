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
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
    return true;
  }
 
  // click KeepEditing  button of the Institution Not Saved Modal
  async clickKeepEditingButton(): Promise<void> {
    await this.clickButton(LinkText.KeepEditing);
  }
  // click yes, Leave button of the Institution Not Saved Modal
  async clickYesLeaveButton(): Promise<Page> {
    await this.clickButton(LinkText.YesLeave);
    return page;
  }
}