import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

export default class InputNumber extends BaseElement {

  static async findByName(page: Page,
                           xOpt: XPathOptions,
                           container?: Container,
                           waitOptions: WaitForSelectorOptions = {visible: true}): Promise<InputNumber> {

    xOpt.type = ElementType.Number;
    const xpath = xPathOptionToXpath(xOpt, container);
    const input = new InputNumber(page, xpath);
    await input.waitForXPath(waitOptions);
    return input;
  }

}
