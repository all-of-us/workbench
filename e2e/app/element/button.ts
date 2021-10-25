import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

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
}
