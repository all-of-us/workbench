import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class InputNumber extends BaseElement {
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): InputNumber {
    xOpt.type = ElementType.Number;
    const xpath = buildXPath(xOpt, container);
    const input = new InputNumber(page, xpath);
    return input;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }
}
