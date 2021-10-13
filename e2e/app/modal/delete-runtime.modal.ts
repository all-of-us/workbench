import { Page } from 'puppeteer';
import { LinkText } from 'app/text-labels';
import Modal from './modal';
import { waitForText } from 'utils/waits-utils';

const modalTitle = 'Delete Runtime';

export default class DeleteRuntimeModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  // click Delete  button of the Delete RuntimeModal
  async clickDeleteButton(): Promise<void> {
    await this.clickButton(LinkText.Delete);
  }
  
  // click Cancel button of the Delete Runtime Modal
  async clickCancelButton(): Promise<void> {
    await this.clickButton(LinkText.Cancel);
  }
}