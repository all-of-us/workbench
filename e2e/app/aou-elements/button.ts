import {JSHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './text-options';
import BaseElement from './base-element';
import {findButton} from './xpath-finder';

export default class Button extends BaseElement {

  static async forLabel(
     page: Page,
     textOptions: TextOptions,
     waitOptions: WaitForSelectorOptions = { visible: true },
     throwErr = true): Promise<Button> {

    let element: Button;
    try {
      const buttonElement = await findButton(page, textOptions, waitOptions);
      element = new Button(page, buttonElement);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding Button: "${JSON.stringify(textOptions)}".`);
        throw e;
      }
    }
    return element;
  }

  /**
   * Wait until button is clickable (enabled).
   */
  async waitUntilEnabled(): Promise<JSHandle> {
    const handle = this.element.asElement();
    return await handle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style.getPropertyValue('cursor') === 'pointer';
    }, this.element);
  }

}
