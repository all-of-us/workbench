import {Page} from 'puppeteer';
import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';
import {getPropValue} from 'utils/element-utils';
import BaseElement from './base-element';
import {buildXPath} from 'app/xpath-builders';

/**
 * <select> element
 */
export default class Select extends BaseElement {

  private selectedOption: string;
   
  static async findByName(page: Page, xOpt: XPathOptions, container?: Container): Promise<Select> {
    xOpt.type = ElementType.Select;
    const selectXpath = buildXPath(xOpt, container);
    const select = new Select(page, selectXpath);
    return select;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async selectOption(optionValue: string): Promise<string> {
    [this.selectedOption] = await (await this.asElementHandle()).select(optionValue);
    return this.selectedOption;
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedOption(): Promise<string> {
    const selector = this.xpath + '/parent::*/following-sibling::label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    return this.selectedOption = await getPropValue<string>(displayedValue, 'innerText');
  }

}
