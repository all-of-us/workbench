import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from './container';
import TextOptions from './text-options';
import BaseElement from './base-element';
import * as xpathDefaults from './xpath-defaults';

export default class Textarea extends BaseElement {

  static async forLabel(
     pageOptions: {puppeteerPage: Page, container?: Container},
     textOptions: TextOptions,
     waitOptions?: WaitForSelectorOptions): Promise<Textarea> {

    if (textOptions.ancestorNodeLevel === undefined) {
      textOptions.ancestorNodeLevel = 2;
    }
    const textareaXpath = `${xpathDefaults.labelXpath(textOptions, pageOptions.container)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//textarea`;
    const textarea = new Textarea({puppeteerPage: pageOptions.puppeteerPage}, {xpath: textareaXpath});
    await textarea.findFirstElement(waitOptions);
    return textarea;
  }

}
