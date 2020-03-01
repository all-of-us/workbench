import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findLabel} from './xpath-finder';

export default class Label extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    throwErr = throwErr || true;
    try {
      this.element = await findLabel(this.page, textOptions, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Text: "${textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

}
