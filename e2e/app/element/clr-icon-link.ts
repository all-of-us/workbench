import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

export default class ClrIconLink extends BaseElement {
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<ClrIconLink> {
    xOpt.type = ElementType.Icon;
    const iconXpath = buildXPath(xOpt, container);
    const iconLink = new ClrIconLink(page, iconXpath);
    return iconLink;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  /**
   * Is Icon disabled?
   * clr-icon itself cannot tell us if it is disabled. We have to use parent element.
   */
  async isDisabled(): Promise<boolean> {
    const selector = `${this.xpath}/ancestor::node()[1]`;
    const elemt = await this.page.waitForXPath(selector, { visible: true });
    return ClrIconLink.asBaseElement(this.page, elemt).isCursorNotAllowed();
  }
}
