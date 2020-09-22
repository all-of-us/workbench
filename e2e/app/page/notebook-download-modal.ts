import {ElementHandle, Frame, Page} from 'puppeteer';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';

enum Xpath {
  modal = '//*[@role="dialog"]',
  policyCheckbox = '//*[@id="confirm-policy"]',
  downloadButton = '//*[@id="aou-download"]',
}

// Note: this does not extended the standard e2e Modal component because it
// assumes the Jupyter UI is running in an iframe. It therefore needs to operate
// on a frame, rather than on the global page.
export default class NotebookDownloadModal {

  constructor(private page: Page, private frame: Frame) {}

  async waitUntilVisible(): Promise<ElementHandle> {
    return await this.frame.waitForXPath(Xpath.modal, {visible: true});
  }

  async waitForLoad(): Promise<this> {
    try {
      await this.waitUntilVisible();
    } catch (e) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      const title = await this.page.title();
      throw new Error(`"${title}" modal waitForLoad() encountered ${e}`);
    }
    return this;
  }

  async waitUntilClose(): Promise<void> {
    await this.frame.waitForXPath(Xpath.modal, {hidden: true});
  }

  async clickPolicyCheckbox(): Promise<void> {
    const checkbox = await this.getPolicyCheckbox();

    // XXX: All of this fails to actually dynamically update the checkbox value in
    // headless mode. Simply clicking the element works fine in debug mode.
    await checkbox.focus();
    await checkbox.hover();
    await checkbox.click();
    console.log(await this.frame.evaluate(e => {
      console.log(e);
      return (window as any).$(e).change();
    }, checkbox));
    await this.page.waitFor(500);
  }

  async getPolicyCheckbox(): Promise<ElementHandle> {
    return this.frame.waitForXPath(Xpath.modal + Xpath.policyCheckbox, {visible: true});
  }

  async getDownloadButton(): Promise<ElementHandle> {
    return this.frame.waitForXPath(Xpath.modal + Xpath.downloadButton, {visible: true});
  }
}
