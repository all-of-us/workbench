import {Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import TextOptions from './text-options';
import {findIcon} from './xpath-finder';

export default class ClrIconLink extends BaseElement {
   
  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     shape: string,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<ClrIconLink> {

    let element: ClrIconLink;
    try {
      const iconElement = await findIcon(page, textOptions, shape, waitOptions);
      element = new ClrIconLink(page, iconElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a clr-icon: "${textOptions}".`);
        throw e;
      }
    }
    return element;
  }

}
