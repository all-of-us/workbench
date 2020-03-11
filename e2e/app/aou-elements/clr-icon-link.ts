import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import {findIcon} from './xpath-finder';


export default class ClrIconLink extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(aElementName: string, shape: string, options?: WaitForSelectorOptions, throwErr = true): Promise<ElementHandle> {
    try {
      this.element = await findIcon(this.page, aElementName, shape, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a 'clr-icon': "${aElementName}".`);
        throw e;
      }
    }
    return this.element;
  }

}
