import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class Link extends BaseElement {
   
  static async findByName(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<Link> {

    xOpt.type = ElementType.Link;
    const linkXpath = xPathOptionToXpath(xOpt, container);
    const link = new Link(page, linkXpath);
    await link.waitForXPath(waitOptions);
    return link;
  }


}
