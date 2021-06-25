import { ElementHandle, Frame, Page } from 'puppeteer';
import { savePageToFile, takeScreenshot } from 'utils/save-file-utils';

enum Xpath {
  modal = '//*[@role="dialog"]',
  policyCheckbox = '//*[@id="confirm-policy"]',
  downloadButton = '//*[@id="aou-download"]'
}

// Note: this does not extended the standard e2e Modal component because it
// assumes the Jupyter UI is running in an iframe. It therefore needs to operate
// on a frame, rather than on the global page.
export default class NotebookDownloadModal {
  constructor(private page: Page, private frame: Frame) {}

  async waitUntilVisible(): Promise<ElementHandle> {
    return await this.frame.waitForXPath(Xpath.modal, { visible: true });
  }

  async waitForLoad(): Promise<this> {
    try {
      await this.waitUntilVisible();
    } catch (err) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      throw err;
    }
    return this;
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
