import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {buttonXpath} from './xpath-defaults';

export default class Button extends BaseElement {

  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<Button> {

    const xpath = buttonXpath(textOptions, pageOptions.container);
    const button = new Button({puppeteerPage: pageOptions.puppeteerPage}, {xpath});
    await button.findFirstElement(waitOptions);
    return button;
  }

  /**
   * Wait until button is clickable (enabled).
   * @param {string} selector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  async waitUntilEnabled(selector?: string): Promise<void> {
    // works with either a xpath selector or a Element
    if (selector === undefined) {
      await this.puppeteerPage.waitForFunction((e) => {
        const style = window.getComputedStyle(e);
        return style.getPropertyValue('cursor') === 'pointer';
      }, {}, this.element);
      return;
    }

    await this.puppeteerPage.waitForFunction(xpathSelector => {
      const elemt = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      const style = window.getComputedStyle(elemt as Element);
      const propValue = style.getPropertyValue('cursor');
      return propValue === 'pointer';
    }, {}, selector);
  }

}
