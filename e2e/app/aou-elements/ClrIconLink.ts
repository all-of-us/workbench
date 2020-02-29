import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findClrIcon} from './xpath-finder';

export default class ClrIconLink extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findClrIcon(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a 'clr-icon' link: "${this.name}".`);
        throw e;
      }
    }
    return this.element;
  }

}
