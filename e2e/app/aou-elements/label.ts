import {Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findLabel} from './xpath-finder';

export default class Label extends BaseElement {

  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = {visible: true},
     throwErr = true): Promise<Label> {

    let element: Label;
    try {
      const labelElement = await findLabel(page, textOptions, waitOptions);
      element = new Label(page, labelElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Label: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
  }

}
