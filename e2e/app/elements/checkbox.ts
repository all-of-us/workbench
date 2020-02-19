import {Page} from 'puppeteer';
import * as elementHandler from '../../driver/elementHandle-util';
import Widget from './widget';

export default class Checkbox extends Widget {
  public label: string;

  constructor(page: Page, label: string) {
    super(page);
    this.label = label;
  }

  public async get() {
    return await this.findCheckbox(this.label);
  }

  public async click() {
    const checkbox = await this.get();
    await checkbox.click();
      // TODO add getProperty to check element state changed
  }

  public async isChecked(): Promise<boolean> {
    const checkbox = await this.get();
    return !!(await elementHandler.getProperty(this.puppeteerPage, checkbox, 'checked'))
  }

  /**
   * Make checkbox element checked
   */
  public async check() {
    if (!this.isChecked()) {
      await this.click();
    }
  }

  /**
   * Make checkbox element unchecked
   */
  public async uncheck() {
    if (this.isChecked()) {
      await this.click();
    }
  }

  public async isDisabled() {
    const checkbox = await this.get();
    await elementHandler.getProperty(this.puppeteerPage, checkbox, 'disabled');
  }

}
