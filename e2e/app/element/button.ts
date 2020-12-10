import {Page} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import {getPropValue} from 'utils/element-utils';
import {waitForFn} from 'utils/waits-utils';
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
  async waitUntilEnabled(): Promise<void> {
    const isCursorPointer = async(): Promise<boolean> => {
      return await getPropValue<string>(await this.asElementHandle(), 'cursor') === 'pointer';
    }
    await waitForFn(isCursorPointer, 2000, 30000);
  }

}
