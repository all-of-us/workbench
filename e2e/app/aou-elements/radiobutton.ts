import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findRadiobutton} from './xpath-finder';

export default class RadioButton extends BaseElement {
   
  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<RadioButton> {

    let element: RadioButton;
    try {
      const radioElement = await findRadiobutton(page, textOptions, waitOptions);
      element = new RadioButton(page, radioElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Radiobutton: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
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
