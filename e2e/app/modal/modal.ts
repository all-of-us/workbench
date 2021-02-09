import {Page} from 'puppeteer';
import BaseModal from './base-modal';

export default class Modal extends BaseModal {

   constructor(page: Page, xpath?: string) {
      super(page, xpath);
   }

   isLoaded(): Promise<boolean> {
      return Promise.resolve(true);
   }

}
