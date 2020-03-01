import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findButton} from './xpath-finder';

export default class Button extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    throwErr = throwErr || true;
    try {
      this.element = await findButton(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Button: "${textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

  /**
   * Determine if button is disabled by checking style 'cursor'.
   */
  public async isCursorNotAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return cursor === 'not-allowed';
  }

}
