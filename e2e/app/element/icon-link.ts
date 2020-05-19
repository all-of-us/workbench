import {Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import Container from './container';
import TextOptions from './text-options';
import {clrIconXpath} from './xpath-defaults';

export default class IconLink extends BaseElement {
   
  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     shape: string,
     waitOptions?: WaitForSelectorOptions): Promise<IconLink> {

    const iconXpath = clrIconXpath(textOptions, shape, pageOptions.container);
    const iconLink = new IconLink({puppeteerPage: pageOptions.puppeteerPage}, {xpath: iconXpath});
    await iconLink.findFirstElement(waitOptions);
    return iconLink;
  }

}
