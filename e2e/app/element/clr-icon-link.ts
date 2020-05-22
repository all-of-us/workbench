import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class ClrIconLink extends BaseElement {

  static async forLabel(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<ClrIconLink> {

    xOpt.type = ElementType.Icon;
    const iconXpath = xPathOptionToXpath(xOpt, container);
    const iconLink = new ClrIconLink(page, iconXpath);
    await iconLink.waitForXPath(waitOptions);
    return iconLink;
  }

}
