import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class RadioButton extends BaseElement {
   
  static async findByName(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<RadioButton> {

    xOpt.type = ElementType.RadioButton;
    const radioButtonXpath = xPathOptionToXpath(xOpt, container);
    const radioButton = new RadioButton(page, radioButtonXpath);
    await radioButton.waitForXPath(waitOptions);
    return radioButton;
  }

  async isSelected(): Promise<boolean> {
    await this.focus();
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
  async unSelect(): Promise<void> {
    const is = await this.isSelected();
    if (is) {
      await this.click();
      await this.page.waitFor(500);
    }
  }

}
