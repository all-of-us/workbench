import {Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import {findClickable} from './xpath-finder';

export default class Link extends BaseElement {
   
  static async forLabel(
     page: Page,
     aElementName: string,
     options: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<Link> {

    let element: Link;
    try {
      const linkElement = await findClickable(page, aElementName, options);
      element = new BaseElement(page, linkElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Link: "${aElementName}".`);
        throw e;
      }
    }
    return element;
  }


}
