import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

/**
 * An input element.
 */
export default class Textbox extends BaseElement {
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Textbox> {
    xOpt.type = ElementType.Textbox;
    const textboxXpath = buildXPath(xOpt, container);
    const textbox = new Textbox(page, textboxXpath);
    return textbox;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }
}
