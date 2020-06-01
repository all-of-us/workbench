import {Page, WaitForSelectorOptions} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import BaseElement from './base-element';
import {xPathOptionToXpath} from './xpath-defaults';

/**
 * <select> element
 */
export default class Select extends BaseElement {

  private selectedOption;
   
  static async findByName(
     page: Page,
     xOpt: XPathOptions,
     container?: Container,
     waitOptions: WaitForSelectorOptions = {visible: true}): Promise<Select> {

    xOpt.type = ElementType.Select;
    const selectXpath = xPathOptionToXpath(xOpt, container);
    const select = new Select(page, selectXpath);
    await select.waitForXPath(waitOptions);
    return select;
  }

  async selectOption(optionValue: string): Promise<string> {
    this.selectedOption = await this.element.select(optionValue);
    return this.selectedOption;
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedOption(): Promise<string> {
    const selector = this.xpath + '/parent::*/following-sibling::label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    const innerText = await displayedValue.getProperty('innerText');
    this.selectedOption = (await innerText.jsonValue()).toString();
    return this.selectedOption;
  }

}
