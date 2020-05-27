import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

/**
 * An input element.
 */
export default class Textbox extends BaseElement {

  static async forLabel(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<Textbox> {

    xOpt.type = ElementType.Textbox;
    const textboxXpath = xPathOptionToXpath(xOpt, container);
    const textbox = new Textbox(page, textboxXpath);
    await textbox.waitForXPath(waitOptions);
    return textbox;
  }

}
