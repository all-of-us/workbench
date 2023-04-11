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
   */
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): Button {
    xOpt.type = ElementType.Button;
    return new Button(page, buildXPath(xOpt, container));
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }
}
