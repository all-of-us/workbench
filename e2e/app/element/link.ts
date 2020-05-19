import {Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import Container from './container';
import * as xpathDefaults from './xpath-defaults';

export default class Link extends BaseElement {
   
  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     label: string,
     waitOptions?: WaitForSelectorOptions): Promise<Link> {

    const linkXpath = xpathDefaults.clickableXpath(label, pageOptions.container);
    const link = new Link({puppeteerPage: pageOptions.puppeteerPage}, {xpath: linkXpath});
    await link.findFirstElement(waitOptions);
    return link;
  }

}
