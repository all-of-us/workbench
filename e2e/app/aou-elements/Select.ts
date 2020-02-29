import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findSelect} from './xpath-finder';

export default class Select extends WebElement {

  private selectedOption;

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findSelect(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Select: "${this.name}".`);
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
