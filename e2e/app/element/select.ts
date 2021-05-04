import { Page } from 'puppeteer';
import Container from 'app/container';
import { ElementType, XPathOptions } from 'app/xpath-options';
import { getPropValue } from 'utils/element-utils';
import BaseElement from './base-element';
import { buildXPath } from 'app/xpath-builders';

/**
 * <select> element
 */
export default class Select extends BaseElement {
  private selectedOption: string;

  static findByName(page: Page, xOpt: XPathOptions, container?: Container): Select {
    xOpt.type = ElementType.Select;
    const selectXpath = buildXPath(xOpt, container);
    const select = new Select(page, selectXpath);
    return select;
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async selectOption(value: string): Promise<string> {
    const disabled = await this.isDisabled();
    if (disabled) {
      throw new Error(`Select is disabled. Cannot select option value: "${value}".`);
    }
    const selector = `${this.xpath}/option[text()="${value}"]`;
    await this.page.waitForXPath(selector);
    const option = (await this.page.$x(selector))[0];
    const optionValue = await getPropValue<string>(option, 'value');
    [this.selectedOption] = await (await this.asElementHandle()).select(optionValue);
    return this.selectedOption;
  }

  /**
   * Returns value that matches the Select option.
   */
  async getOptionValue(option: string): Promise<string> {
    // Returns option texts. No matter if option was selected or not.
    const selector = `${this.xpath}/option[@value="${option}"]`;
    const displayedValue = await this.page.waitForXPath(selector);
    return getPropValue<string>(displayedValue, 'innerText');
  }

  /**
   * Returns value of Selected option.
   */
  async getSelectedValue(): Promise<string> {
    const selectElement = await this.page.waitForXPath(this.getXpath(), { visible: true });
    const selectedOption = await this.page.evaluate((select) => {
      for (const option of select.options) {
        if (option.selected) {
          return option.textContent;
        }
      }
    }, selectElement);
    return selectedOption;
  }

  /**
   * Wait until value of Selected option equals to expected option.
   */
  async waitForSelectedValue(expectedOption: string, timeout = 30000): Promise<void> {
    const selectElement = await this.page.waitForXPath(this.getXpath(), { visible: true });
    await this.page
      .waitForFunction(
        (select, text) => {
          for (const option of select.options) {
            if (option.selected) {
              console.log(option.textContent);
              return option.textContent === text;
            }
          }
        },
        { timeout },
        selectElement,
        expectedOption
      )
      .catch((err) => {
        console.error(`waitForSelectedValue() failed. Expected selected option is ${expectedOption}`);
        console.error(err);
        throw new Error(err);
      });
  }
}
