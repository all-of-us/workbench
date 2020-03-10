import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findRadiobutton} from './xpath-finder';

export default class RadioButton extends BaseElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  async withLabel(textOptions: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    throwErr = throwErr || true;
    if (waitOptions === undefined) { waitOptions = {visible: true}; }
    try {
      this.element = await findRadiobutton(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Radiobutton: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return this.element;
  }

  async isSelected() {
    await this.element.focus();
    const is = await this.getProperty('checked');
    return !!is;
  }

  /**
   * Select a RadioButton.
   */
  async select(): Promise<void> {
    const is = await this.isSelected();
    if (!is) {
      await this.click();
      await this.page.waitFor(500);
    }
  }

  /**
   * Unselect a RadioButton.
   */
  async unSelect() {
    const is = await this.isSelected();
    if (is) {
      await this.click();
      await this.page.waitFor(500);
    }
  }

}
