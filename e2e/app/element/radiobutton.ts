import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {inputXpath} from './xpath-defaults';

export default class RadioButton extends BaseElement {
   
  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<RadioButton> {

    if (textOptions.ancestorNodeLevel === undefined) {
      textOptions.ancestorNodeLevel = 1;
    }
    textOptions.inputType = 'radio';
    const radioButtonXpath = inputXpath(textOptions, pageOptions.container);
    const radioButton = new RadioButton({puppeteerPage: pageOptions.puppeteerPage}, {xpath: radioButtonXpath});
    await radioButton.findFirstElement(waitOptions);
    return radioButton;
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
      await this.puppeteerPage.waitFor(500);
    }
  }

  /**
   * Unselect a RadioButton.
   */
  async unSelect() {
    const is = await this.isSelected();
    if (is) {
      await this.click();
      await this.puppeteerPage.waitFor(500);
    }
  }

}
