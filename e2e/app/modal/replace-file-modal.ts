import { ElementHandle, Page } from 'puppeteer';
import * as fp from 'lodash/fp';
import { getPropValue } from 'utils/element-utils';
import Modal from './modal';

export enum Xpath {
  modal = '//*[@role="dialog"]',
  overwriteButton = '//button[text()="Overwrite"]',
  cancelButton = '//button[text()="Cancel"]'
}

export default class ReplaceFileModal extends Modal {
  constructor(page: Page, xpath = Xpath.modal) {
    super(page, xpath);
  }

  async clickCancelButton(): Promise<void> {
    const button = await this.getCancelButton();
    await button.click();
  }

  async getCancelButton(): Promise<ElementHandle> {
    return this.page.waitForXPath(Xpath.modal + Xpath.cancelButton, { visible: true });
  }

  async isLoaded(): Promise<boolean> {
    return this.page
      .waitForXPath(Xpath.modal, { visible: true, timeout: 5000 })
      .then(() => this.page.waitForXPath(Xpath.modal + Xpath.overwriteButton, { visible: true, timeout: 100 }))
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }

  async getText(): Promise<string[]> {
    // xpath that excludes button labels and spans
    const selector = `${Xpath.modal}//*[@class="modal-body"]/p`;
    await this.page.waitForXPath(selector, { visible: true });
    const elements: ElementHandle[] = await this.page.$x(selector);
    return fp.flow(
      fp.map(async (elem: ElementHandle) => (await getPropValue<string>(elem, 'innerText')).trim()),
      (contents) => Promise.all(contents)
    )(elements);
  }
}
