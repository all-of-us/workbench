import { ElementHandle, Page } from 'puppeteer';
import NotebookFrame from 'app/page/notebook-frame';

enum Xpath {
  modal = '//*[@role="dialog"]',
  policyCheckbox = '//*[@id="confirm-policy"]',
  downloadButton = '//*[@id="aou-download"]'
}

// Note: this does not extended the standard e2e Modal component because it
// assumes the Jupyter UI is running in an iframe. It therefore needs to operate
// on a frame, rather than on the global page.
export default class NotebookDownloadModal extends NotebookFrame {
  constructor(page: Page) {
    super(page);
  }

  async waitUntilClose(): Promise<void> {
    await (await this.getIFrame()).waitForXPath(Xpath.modal, { hidden: true });
  }

  async clickPolicyCheckbox(): Promise<void> {
    const checkbox = await this.getPolicyCheckbox();
    await checkbox.click();
  }

  async getPolicyCheckbox(): Promise<ElementHandle> {
    return (await this.getIFrame()).waitForXPath(Xpath.modal + Xpath.policyCheckbox, { visible: true });
  }

  async clickDownloadButton(): Promise<void> {
    // Chrome headless is unable to click this button properly using standard
    // ElementHandle.click(). Even a standard element click() doesn't do it,
    // likely because the Jupyter UI heavily uses jQuery event handling.
    await (await this.getIFrame()).evaluate(() => {
      return (window as any).$('#aou-download').click();
    });
  }

  async getDownloadButton(): Promise<ElementHandle> {
    return (await this.getIFrame()).waitForXPath(Xpath.modal + Xpath.downloadButton, { visible: true });
  }

  async isLoaded(): Promise<boolean> {
    return (await this.getIFrame())
      .waitForXPath(Xpath.modal, { visible: true })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }
}
