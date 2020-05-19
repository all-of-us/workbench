import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {inputXpath} from './xpath-defaults';

export default class Checkbox extends BaseElement {
   
  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<Checkbox> {

    if (textOptions.ancestorNodeLevel === undefined) {
      textOptions.ancestorNodeLevel = 1;
    }
    textOptions.inputType = 'checkbox';
    const checkboxXpath = inputXpath(textOptions, pageOptions.container);
    const checkbox = new Checkbox({puppeteerPage: pageOptions.puppeteerPage}, {xpath: checkboxXpath});
    await checkbox.findFirstElement(waitOptions);
    return checkbox;
  }

  /**
   * Checked means element does not have a `checked` property
   */
  async isChecked(): Promise<boolean> {
    const is = await this.getProperty('checked');
    return !!is;
  }

  /**
   * Click on checkbox element for checked
   */
  async check(): Promise<void> {
    const is = await this.isChecked();
    if (!is) {
      await this.focus();
      await this.clickWithEval();
      await this.puppeteerPage.waitFor(500);
    }
    // check and retry ??
  }

  /**
   * Click on checkbox element for unchecked
   */
  async unCheck() {
    const is = await this.isChecked();
    if (is) {
      await this.focus();
      await this.clickWithEval();
      await this.puppeteerPage.waitFor(500);
    }
  }

  /**
   * Toggle checkbox state.
   * @param {boolean} checked
   */
  async toggle(checked?: boolean): Promise<void> {
    if (checked === undefined) {
      await this.clickWithEval();
    }
    if (checked) {
      return this.check();
    }
    return this.unCheck();
  }

}
