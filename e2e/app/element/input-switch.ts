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
      if (isChecked || maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitForTimeout(2000).then(click); // Two seconds pause before try again
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
