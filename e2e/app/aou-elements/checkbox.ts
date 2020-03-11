import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findCheckbox} from './xpath-finder';

export default class Checkbox extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr = true): Promise<ElementHandle> {
    if (waitOptions === undefined) {
      waitOptions = {visible: true};
    }
    try {
      this.element = await findCheckbox(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Checkbox: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

  // find checkbox label by matching label
  async withMatchLabel(checkboxLabel: string): Promise<ElementHandle> {
    const selector = '[type="checkbox"] + label';
    await this.page.waitForSelector(selector);
    const handles = await this.page.$$(selector);
    for (const elem of handles) {
      const innerTxt  = await (await elem.getProperty('innerText')).jsonValue();
      if (innerTxt === checkboxLabel) {
        this.element = elem;
        return elem;
      }
    }
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

}
