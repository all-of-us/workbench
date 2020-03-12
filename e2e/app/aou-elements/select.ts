import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findSelect} from './xpath-finder';

export default class Select extends BaseElement {

  private selectedOption;
   
  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<Select> {

    let elem: Select;
    try {
      const selectElement = await findSelect(page, textOptions, waitOptions);
      elem = new Select(page, selectElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Select: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return elem;
  }

  async selectOption(optionValue: string): Promise<string> {
    this.selectedOption = await this.element.select(optionValue);
    return this.selectedOption;
  }

  getSelectedOption(): string {
    return this.selectedOption;
  }

}
