import { ElementHandle, Frame, Page } from 'puppeteer';
import Modal from './modal';

enum Xpath {
  modal = '//*[@role="dialog"]',
  policyCheckbox = '//*[@id="confirm-policy"]',
  downloadButton = '//*[@id="aou-download"]'
}

// Note: this does not extended the standard e2e Modal component because it
// assumes the Jupyter UI is running in an iframe. It therefore needs to operate
// on a frame, rather than on the global page.
export default class NotebookDownloadModal extends Modal {
  constructor(page: Page, private frame: Frame) {
    super(page, Xpath.modal);
  }

  async isLoaded(): Promise<boolean> {
    await this.frame.waitForXPath(Xpath.modal, { visible: true });
    return true;
  }

  async waitUntilClose(): Promise<void> {
    await this.frame.waitForXPath(Xpath.modal, { hidden: true });
  }

  async clickPolicyCheckbox(): Promise<void> {
    const checkbox = await this.getPolicyCheckbox();
    await checkbox.click();
  }

  async getPolicyCheckbox(): Promise<ElementHandle> {
    return this.frame.waitForXPath(Xpath.modal + Xpath.policyCheckbox, { visible: true });
  }

  async clickDownloadButton(): Promise<void> {
    // Chrome headless is unable to click this button properly using standard
    // ElementHandle.click(). Even a standard element click() doesn't do it,
    // likely because the Jupyter UI heavily uses jQuery event handling.
    await this.frame.evaluate(() => {
      return (window as any).$('#aou-download').click();
    });
  }

  async getDownloadButton(): Promise<ElementHandle> {
    return this.frame.waitForXPath(Xpath.modal + Xpath.downloadButton, { visible: true });
  }
}
