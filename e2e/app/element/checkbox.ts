import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findCheckbox} from './xpath-finder';

export default class Checkbox extends BaseElement {
   
  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<Checkbox> {

    let element: Checkbox;
    try {
      const checkboxElement = await findCheckbox(page, textOptions, waitOptions);
      element = new Checkbox(page, checkboxElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Checkbox: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
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
      await this.page.waitFor(500);
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
      await this.page.waitFor(500);
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
