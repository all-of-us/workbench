import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {buildXPath} from 'app/xpath-builders';

export default class ClrIconLink extends BaseElement {

  static async findByName(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<ClrIconLink> {

    xOpt.type = ElementType.Icon;
    const iconXpath = buildXPath(xOpt, container);
    const iconLink = new ClrIconLink(page, iconXpath);
    await iconLink.waitForXPath(waitOptions);
    return iconLink;
  }

  /**
   * Is Icon disabled?
   * clr-icon itself cannot tell us if it is disabled. We have to use parent element.
   */
  async isDisabled(): Promise<boolean> {
    const selector = `${this.xpath}/ancestor::node()[1]`;
    const elemt = await this.page.waitForXPath(selector);
    return ClrIconLink.asBaseElement(this.page, elemt).isCursorNotAllowed();
  }

}
