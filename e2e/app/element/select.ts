import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {labelXpath} from './xpath-defaults';

export default class Select extends BaseElement {

  private selectedOption;
   
  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<Select> {

    if (textOptions.ancestorNodeLevel === undefined) {
      textOptions.ancestorNodeLevel = 2;
    }
    const selectXpath = `${labelXpath(textOptions, pageOptions.container)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//select`;
    const select = new Select({puppeteerPage: pageOptions.puppeteerPage}, {xpath: selectXpath});
    await select.findFirstElement(waitOptions);
    return select;
  }

  async selectOption(optionValue: string): Promise<string> {
    this.selectedOption = await this.element.select(optionValue);
    return this.selectedOption;
  }

  getSelectedOption(): string {
    return this.selectedOption;
  }

}
