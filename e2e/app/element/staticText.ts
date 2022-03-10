import BaseElement from './base-element';
import { Page } from 'puppeteer';
import { ElementType, XPathOptions } from 'app/xpath-options';
import Container from 'app/container';
import { buildXPath } from 'app/xpath-builders';

export default class StaticText extends BaseElement {
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): StaticText {
    xOpt.type = ElementType.StaticText;
    const xpath = buildXPath(xOpt, container);
    return new StaticText(page, xpath);
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async getText(): Promise<string> {
    return this.getProperty('innerText');
  }
}
