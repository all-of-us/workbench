import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class Link extends BaseElement {
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Link> {
    xOpt.type = ElementType.Link;
    const linkXpath = buildXPath(xOpt, container);
    const link = new Link(page, linkXpath);
    return link;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }
}
