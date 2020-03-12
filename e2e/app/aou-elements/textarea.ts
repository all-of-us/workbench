import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findTextarea} from './xpath-finder';

export default class Textarea extends BaseElement {

  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = { visible: true },
     throwErr = true): Promise<Textarea> {

    let element: Textarea;
    try {
      const textareaElement = await findTextarea(page, textOptions, waitOptions);
      element = new Textarea(page, textareaElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Textarea: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
  }

}
