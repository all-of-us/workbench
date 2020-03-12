import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findTextbox} from './xpath-finder';

export default class Textbox extends BaseElement {

  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<Textbox> {

    let element: Textbox;
    try {
      const textboxElement = await findTextbox(page, textOptions, waitOptions);
      element = new Textbox(page, textboxElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Textbox: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
  }

}
