import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findText} from './xpath-finder';

export default class Text extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findText(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Text: "${this.name}".`);
        throw e;
      }
    }
    return this.element;
  }

}
