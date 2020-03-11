import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findSelect} from './xpath-finder';

export default class Select extends BaseElement {

  private selectedOption;

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr = true): Promise<ElementHandle> {
    if (waitOptions === undefined) {
      waitOptions = {visible: true};
    }
    try {
      this.element = await findSelect(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Select: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

  async selectOption(optionValue: string): Promise<string> {
    this.selectedOption = await this.element.select(optionValue);
    return this.selectedOption;
  }

  getSelectedOption(): string {
    return this.selectedOption;
  }

}
