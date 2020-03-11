import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findButton} from './xpath-finder';

export default class Button extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr = true): Promise<ElementHandle> {
    if (waitOptions === undefined) {
      waitOptions = {visible: true};
    }
    try {
      this.element = await findButton(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Button: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

}
