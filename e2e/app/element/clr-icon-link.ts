import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import BaseElement from './base-element';
import TextOptions from './text-options';
import {findIcon} from './xpath-finder';

export default class ClrIconLink extends BaseElement {
   
  static async forLabel(
     parentNode: {pageInstance: Page, element?: ElementHandle},
     textOptions: TextOptions,
     shape: string,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<ClrIconLink> {

    let element: ClrIconLink;
    try {
      const iconElement = await findIcon(parentNode, textOptions, shape, waitOptions);
      element = new ClrIconLink(parentNode.pageInstance, iconElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding a clr-icon: "${textOptions}".`);
        throw e;
      }
    }
    return element;
  }

}
