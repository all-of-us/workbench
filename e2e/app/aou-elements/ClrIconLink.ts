import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findClrIcon} from './xpath-finder';

export default class ClrIconLink extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementLabel: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.labelText = aElementLabel;
    throwErr = throwErr || true;
    try {
      this.element = await findClrIcon(this.page, this.labelText, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a 'clr-icon' link: "${this.labelText}".`);
        throw e;
      }
    }
    return this.element;
  }

}
