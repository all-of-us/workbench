import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class Checkbox extends BaseElement {
   
  static async findByName(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<Checkbox> {

    xOpt.type = ElementType.Checkbox;
    const checkboxXpath = xPathOptionToXpath(xOpt, container);
    const checkbox = new Checkbox(page, checkboxXpath);
    await checkbox.waitForXPath(waitOptions);
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
    await this.unCheck();
    return;
  }

}
