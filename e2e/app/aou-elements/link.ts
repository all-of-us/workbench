import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './web-element';
import {findClickable} from './xpath-finder';

export default class Link extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (options === undefined) { options = {visible: true}; }
    try {
      this.element = await findClickable(this.page, aElementName, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Link: "${aElementName}".`);
        throw e;
      }
    }
    return this.element;
  }

  /**
   * Determine if button is disabled by checking style 'cursor'.
   */
  async isCursorAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return cursor === 'not-allowed';
  }

}
