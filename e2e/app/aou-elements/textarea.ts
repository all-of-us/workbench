import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findTextarea} from './xpath-finder';

export default class Textarea extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (waitOptions === undefined) { waitOptions = {visible: true}; }
    try {
      this.element = await findTextarea(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Textarea: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

}
