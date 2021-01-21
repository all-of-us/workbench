import Container from 'app/container';
import {buildXPath} from 'app/xpath-builders';
import {ElementType, XPathOptions} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';
import BaseMenu from './base-menu';

const defaultMenuXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown ")]' +
   '[.//*[contains(concat(" ", normalize-space(@class), " "), " p-input-overlay-visible ")]]';

export default class SelectMenu extends BaseMenu {

  static async findByName(page: Page, xOpt: XPathOptions = {}, container?: Container): Promise<SelectMenu> {
    xOpt.type = ElementType.Dropdown;
    const menuXpath = buildXPath(xOpt, container);
    const selectMenu = new SelectMenu(page, menuXpath);
    return selectMenu;
  }

  constructor(page: Page, xpath: string  = defaultMenuXpath) {
    super(page, xpath);
  }

  /**
   * Select an option in a Select element.
   * @param {string} textValue Partial or full string to click in Select.
   * @param {number} maxAttempts Default try count is 2.
   */
  async selectOption(textValue: string, maxAttempts: number = 2): Promise<void> {

    const clickText = async () => {
      await this.open(2);
      const link = await this.findMenuItemLink(textValue, this.getXpath());
      const textContent = await getPropValue<string>(await link.asElementHandle(), 'textContent');
      await link.click();
      return textContent;
    };

    const clickAndCheck = async () => {
      maxAttempts--;
      const shouldSelectedValue = await clickText();
      // compare to displayed text in Select textbox
      const displayedValue = await this.getSelectedValue();
      if (shouldSelectedValue === displayedValue) {
        return shouldSelectedValue; // success
      }
      if (maxAttempts <= 0) {
        return null;
      }
      return await this.page.waitForTimeout(2000).then(clickAndCheck); // two seconds pause and retry
    };

    await clickAndCheck();
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedValue(): Promise<string> {
    const selector = this.xpath + '//*[contains(normalize-space(@class), "p-inputtext")]';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    return getPropValue<string>(displayedValue, 'innerText');
  }

  /**
   *  Get texts of all visible options.
   */
  async getAllOptionTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}//*[@role="option"]/text()`;
    return this.getMenuItemTexts(selector);
  }

  /**
   * Open Select dropdown.
   * @param {number} maxAttempts Default is 2.
   */
  private async open(maxAttempts: number = 2): Promise<void> {
    const click = async () => {
      await this.toggle();
      const opened = await this.isOpen();
      if (opened) {
        return;
      }
      if (maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitForTimeout(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggle(): Promise<void> {
    const selector = this.xpath + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  getMenuItemXpath(menuItemText: string): string {
    return `//*[@role="option" and normalize-space()="${menuItemText}"]`;
  }

}
