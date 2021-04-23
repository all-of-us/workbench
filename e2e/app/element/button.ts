import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';
import { withErrorLogging } from 'utils/error-handling';

export default class Button extends BaseElement {
  /**
   * @param {Page} page Puppeteer Page.
   * @param {XPathOptions} xOpt Convert XpathOptions to Xpath string.
   * @param {Container} container Parent node if one exists. Normally, it is a Dialog or Modal window.
   * @param {WaitForSelectorOptions} waitOptions.
   */
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): Button {
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
   * @param {string} xpathSelector (Optional) Button Xpath selector.
   * @throws Timeout exception if button is not enabled after waiting.
   */
  waitUntilEnabled = withErrorLogging({
    fn: async (xpathSelector?: string): Promise<void> => {
      const selector = xpathSelector || this.getXpath();
      await this.page.waitForXPath(selector, { visible: true });
      await this.page.waitForFunction(
        (xpath) => {
          const element = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue;
          const style = window.getComputedStyle(element as Element);
          const propValue = style.getPropertyValue('cursor');
          return propValue && propValue === 'pointer';
        },
        {},
        selector
      );
    }
  });
}
