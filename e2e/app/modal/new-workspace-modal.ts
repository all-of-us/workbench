import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Modal from './modal';

const modalTitle = '(Create|Update|Duplicate) Workspace'; // regex expression

export default class NewWorkspaceModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }
}
