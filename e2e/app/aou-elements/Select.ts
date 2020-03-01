import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findSelect} from './xpath-finder';

export default class Select extends WebElement {

  private selectedOption;

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    throwErr = throwErr || true;
    try {
      this.element = await findSelect(this.page, textOptions, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Select: "${textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

  public async selectOption(optionValue: string): Promise<string> {
    this.selectedOption = await this.element.select(optionValue);
    return this.selectedOption;
  }

  public getSelectedOption(): string {
    return this.selectedOption;
  }

}
