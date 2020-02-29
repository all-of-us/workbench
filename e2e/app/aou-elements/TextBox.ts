import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findTextBox} from './xpath-finder';

export default class TextBox extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findTextBox(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding TextBox: "${this.name}".`);
        throw e;
      }
    }
    return this.element;
  }

}
