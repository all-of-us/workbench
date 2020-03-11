import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findTextbox} from './xpath-finder';

export default class Textbox extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }

  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr = true): Promise<ElementHandle> {
    if (waitOptions === undefined) {
      waitOptions = {visible: true};
    }
    try {
      this.element = await findTextbox(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Textbox: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

}
