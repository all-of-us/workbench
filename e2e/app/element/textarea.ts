import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class Textarea extends BaseElement {

  static async forLabel(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = { visible: true }): Promise<Textarea> {

    xOpt.type = ElementType.Textarea;
    const textareaXpath = xPathOptionToXpath(xOpt, container);
    const textarea = new Textarea(page, textareaXpath);
    await textarea.waitForXPath(waitOptions);
    return textarea;
  }

}
