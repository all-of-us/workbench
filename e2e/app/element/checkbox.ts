import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class Checkbox extends BaseElement {
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Checkbox> {
    xOpt.type = ElementType.Checkbox;
    const checkboxXpath = buildXPath(xOpt, container);
    const checkbox = new Checkbox(page, checkboxXpath);
    return checkbox;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  /**
   * Checked means element does not have "checked" property.
   */
  async isChecked(): Promise<boolean> {
    return this.getProperty<boolean>('checked');
  }

  /**
   * Click (check) checkbox element.
   */
  async check(maxAttempts: number = 2): Promise<void> {
    const click = async () => {
      await this.clickWithEval();
      await this.page.waitForTimeout(500);
      const isChecked = await this.isChecked();
      if (isChecked) {
        return;
      }
      if (maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitForTimeout(2000).then(click); // Two seconds pause before try again
    };
    const checked = await this.isChecked();
    if (!checked) {
      await click();
    }
  }

  /**
   * Click on checkbox element for unchecked
   */
  async unCheck() {
    const is = await this.isChecked();
    if (is) {
      await this.focus();
      await this.clickWithEval();
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * Toggle checkbox state.
   * @param {boolean} checked
   */
  async toggle(checked?: boolean): Promise<void> {
    if (checked === undefined) {
      return this.clickWithEval();
    }
    if (checked) {
      return this.check();
    }
    return this.unCheck();
  }
}
