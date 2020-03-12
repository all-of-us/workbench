import {Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import {findIcon} from './xpath-finder';

export default class ClrIconLink extends BaseElement {
   
  static async forLabel(
     page: Page,
     aElementName: string,
     shape: string,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<ClrIconLink> {

    let element: ClrIconLink;
    try {
      const iconElement = await findIcon(page, aElementName, shape, waitOptions);
      element = new ClrIconLink(page, iconElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a clr-icon: "${aElementName}".`);
        throw e;
      }
    }
    return element;
  }

}
