import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import WebElement from './web-element';
import {findTextbox} from './xpath-finder';

export default class Textbox extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (waitOptions === undefined) { waitOptions = {visible: true}; }
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
