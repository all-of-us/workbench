import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import * as xpathDefaults from './xpath-defaults';

export default class Textbox extends BaseElement {

  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<Textbox> {

    if (textOptions.ancestorNodeLevel === undefined) {
      textOptions.ancestorNodeLevel = 1;
    }
    textOptions.inputType = 'text';
    const textboxXpath = xpathDefaults.inputXpath(textOptions, pageOptions.container);
    const textbox = new Textbox({puppeteerPage: pageOptions.puppeteerPage}, {xpath: textboxXpath});
    await textbox.findFirstElement(waitOptions);
    return textbox;
  }

}
