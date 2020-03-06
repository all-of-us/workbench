import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import WebElement from './web-element';
import {findButton} from './xpath-finder';

export default class Button extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (waitOptions === undefined) { waitOptions = {visible: true}; }
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

  /**
   * Determine if button is disabled by checking style 'cursor'.
   */
  async isCursorNotAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return cursor === 'not-allowed';
  }

}
