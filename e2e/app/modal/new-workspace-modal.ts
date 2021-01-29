import {Page} from 'puppeteer';
import {waitForText} from 'utils/waits-utils';
import Modal from './modal';

const modalTitle = '(Create|Update|Duplicate) Workspace'; // regex expression

export default class NewWorkspaceModal extends Modal {

   constructor(page: Page, xpath?: string) {
      super(page, xpath);
   }

   async isLoaded(): Promise<boolean> {
      await waitForText(this.page, modalTitle, {xpath: this.getXpath()});
      return true;
   }

}
