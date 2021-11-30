import { Page } from 'puppeteer';
import BaseModal from './base-modal';

export default class Modal extends BaseModal {
  constructor(page: Page, xpath?: string, opts?: { modalIndex: 1 }) {
    super(page, xpath, opts);
  }

  isLoaded(): Promise<boolean> {
    return Promise.resolve(true);
  }
}
