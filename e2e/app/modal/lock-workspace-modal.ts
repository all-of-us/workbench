import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Modal from './modal';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Textarea from 'app/element/textarea';

const modalTitle = 'Lock workspace';

export default class LockWorkspaceModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  // click cancel button of the lock workspace modal
  clickCancelButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Cancel });
  }

  getLockWorkspaceButton(): Button {
    return Button.findByName(this.page, { name: LinkText.LockWorkspace });
  }

  // click lock workspace button of the modal
  async clickLockWorkspaceButton(): Promise<void> {
    await this.clickButton(LinkText.LockWorkspace);
  }

  getLockWorkspaceTextArea(): Textarea {
    const selector = `${this.getXpath()}//textarea`;
    const lockWorkspaceTextArea = new Textarea(this.page, selector);
    return lockWorkspaceTextArea;
  }

  /**
   * @param {string} newMessage new message.
   */
  async createLockWorkspaceMessage(newMessage?: string): Promise<void> {
    newMessage = "locking this workspace";
    // Type new message.
    const messageInput = this.getLockWorkspaceTextArea();
    await messageInput.type(newMessage);
  }
}
