import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findText} from './xpath-finder';

export default class Text extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementLabel: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.labelText = aElementLabel;
    throwErr = throwErr || true;
    try {
      this.element = await findText(this.page, this.labelText, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Text: "${this.labelText}".`);
        throw e;
      }
    }
    return this.element;
  }

}
