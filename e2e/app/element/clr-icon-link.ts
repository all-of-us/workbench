import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';
import { withErrorLogging } from '../../utils/error-handling';

export default class ClrIconLink extends BaseElement {
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): ClrIconLink {
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
    return withErrorLogging({
      fn: async (): Promise<boolean> => {
        const element = await this.page.waitForXPath(selector, { visible: true });
        return element && ClrIconLink.asBaseElement(this.page, element).isCursorNotAllowed();
      }
    });
  }
}
