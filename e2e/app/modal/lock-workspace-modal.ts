import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Modal from './modal';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Textarea from 'app/element/textarea';

const modalTitle = 'Lock workspace';

export default class LockWorkspaceModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  // get cancel button of the lock workspace modal
  getCancelButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Cancel });
  }

  getLockWorkspaceButton(): Button {
    return Button.findByName(this.page, { name: LinkText.LockWorkspace });
  }

  getLockWorkspaceTextArea(): Textarea {
    const selector = `${this.getXpath()}//textarea`;
    const lockWorkspaceTextArea = new Textarea(this.page, selector);
    return lockWorkspaceTextArea;
  }

  /**
   * @param {string} reasonText reason Text.
   */
  async createLockWorkspaceReason(reasonText?: string): Promise<void> {
    // Type new message.
    const reasonInput = this.getLockWorkspaceTextArea();
    await reasonInput.type(reasonText);
  }
  

  async clickCancelButton(): Promise<void> {
    this.getCancelButton();
  }

  async clickModalLockWorkspace(): Promise<void> {
    await this.clickButton(LinkText.LockWorkspace);
  }

}
