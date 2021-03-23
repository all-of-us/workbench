import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class Textarea extends BaseElement {
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Textarea> {
    xOpt.type = ElementType.Textarea;
    const textareaXpath = buildXPath(xOpt, container);
    const textarea = new Textarea(page, textareaXpath);
    return textarea;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }
}
