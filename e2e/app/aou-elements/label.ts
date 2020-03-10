import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findLabel} from './xpath-finder';

export default class Label extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (waitOptions === undefined) { waitOptions = {visible: true}; }
    try {
      this.element = await findLabel(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Label: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

}
