import {JSHandle, Page} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {buildXPath} from 'app/xpath-builders';

export default class Button extends BaseElement {

  /**
   * @param {Page} page Puppeteer Page.
   * @param {XPathOptions} xOpt Convert XpathOptions to Xpath string.
   * @param {Container} container Parent node if one exists. Normally, it is a Dialog or Modal window.
   * @param {WaitForSelectorOptions} waitOptions.
   */
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Button> {
    xOpt.type = ElementType.Button;
    const butnXpath = buildXPath(xOpt, container);
    const button = new Button(page, butnXpath);
    return button;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  /**
   * Wait until button is clickable (enabled).
   * @param {string} selector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  async waitUntilEnabled(selector?: string): Promise<JSHandle> {
    // works with either a xpath selector or a Element
    if (selector === undefined) {
      return this.asElementHandle().then((elemt) => {
        return this.page.waitForFunction((e) => {
          const style = window.getComputedStyle(e);
          return style.getPropertyValue('cursor') === 'pointer';
        }, {}, elemt);
      });
    }

    return this.page.waitForFunction(xpathSelector => {
      const elemt = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      const style = window.getComputedStyle(elemt as Element);
      const propValue = style.getPropertyValue('cursor');
      return propValue === 'pointer';
    }, {}, selector);
  }

}
