import {Page} from 'puppeteer';
import {LinkText} from 'app/text-labels';
import Modal from './modal';
import {waitForText} from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';

const modalTitle = 'Edit or Delete Review-Wide Annotation Fields';

export default class EditDeleteAnnotationsModal extends Modal {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
     await waitForText(this.page, modalTitle, {xpath: this.getXpath()});
     return true;
  }

   /**
    * @param {string} newName New name.
    */
  
  // rename annotations and close modal window
  async renameAnnotation(newName?: string): Promise<void> {
     await this.clickButton(LinkText.Rename);
      
     const selector = `${this.getXpath()}//*[contains(text(), "Edit")]/following::div[3]/input`;

     const clearTextSelector = await this.page.waitForXPath(selector, {visible: true});
     const baseElement = BaseElement.asBaseElement(this.page, clearTextSelector);
     await baseElement.clearTextInput();
     await baseElement.type(newName);
     await this.clickButton(LinkText.Rename);
     await this.clickButton(LinkText.Close);
     await this.waitUntilClose();
  }

  async deleteAnnotationsName(): Promise<void>{
    await this.clickButton(LinkText.Delete);
    await this.clickButton(LinkText.Yes, {waitForClose: true});

  }
}
