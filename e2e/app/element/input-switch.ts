import BaseElement from './base-element';
import { Page } from 'puppeteer';

export default class InputSwitch extends BaseElement {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isChecked(): Promise<boolean> {
    return this.getProperty<boolean>('ariaChecked');
  }

  async check(maxAttempts = 2): Promise<void> {
    const click = async () => {
      await this.focus();
      await this.click();
      await this.page.waitForTimeout(500);
      const isChecked = await this.isChecked();
      if (isChecked) {
        return;
      }
      if (maxAttempts <= 0) {
        throw Error('maxAttempts reached in input-switch.check().click()');
      }
      maxAttempts--;
      await this.page.waitForTimeout(1000).then(click); // 1 second pause before try again
    };
    const checked = await this.isChecked();
    if (checked) {
      await click();
    }
  }

  async unCheck(): Promise<void> {
    const is = await this.isChecked();
    if (is) {
      await this.focus();
      await this.click();
      await this.page.waitForTimeout(500);
    }
  }
}
