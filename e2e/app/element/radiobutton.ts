import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class RadioButton extends BaseElement {
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): RadioButton {
    xOpt.type = ElementType.RadioButton;
    const radioButtonXpath = buildXPath(xOpt, container);
    const radioButton = new RadioButton(page, radioButtonXpath);
    return radioButton;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isSelected(): Promise<boolean> {
    await this.focus();
    return this.getProperty<boolean>('checked');
  }

  /**
   * Select a RadioButton.
   */
  async select(): Promise<void> {
    const is = await this.isSelected();
    if (!is) {
      await this.click();
      await this.page.waitForTimeout(500);
    }
  }

  /**
   * Unselect a RadioButton.
   */
  async unSelect(): Promise<void> {
    const is = await this.isSelected();
    if (is) {
      await this.click();
      await this.page.waitForTimeout(500);
    }
  }
}
