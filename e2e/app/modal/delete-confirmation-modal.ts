import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';

const modalTitle = 'Are you sure you want to delete';

export default class DeleteConfirmationModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }
}
