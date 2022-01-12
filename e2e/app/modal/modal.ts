import { Page } from 'puppeteer';
import BaseModal from './base-modal';

export default class Modal extends BaseModal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await this.page.waitForXPath(this.getXpath(), { visible: true });
    return true;
  }
}
