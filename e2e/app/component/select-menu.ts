import { Page } from 'puppeteer';
import Container from 'app/container';
import { buildXPath } from 'app/xpath-builders';
import { ElementType, XPathOptions } from 'app/xpath-options';
import { getPropValue } from 'utils/element-utils';
import BaseMenu from './base-menu';

const defaultMenuXpath =
  '//*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown ")]' +
  '[.//*[contains(concat(" ", normalize-space(@class), " "), " p-input-overlay-visible ")]]';

export default class SelectMenu extends BaseMenu {
  static async findByName(page: Page, xOpt: XPathOptions = {}, container?: Container): Promise<SelectMenu> {
    xOpt.type = ElementType.Dropdown;
    const menuXpath = buildXPath(xOpt, container);
    const selectMenu = new SelectMenu(page, menuXpath);
    return selectMenu;
  }

  constructor(page: Page, xpath: string = defaultMenuXpath) {
    super(page, xpath);
  }

  /**
   * Select an option in a Select element.
   * @param {string} textValue Partial or full string to click in Select.
   * @param {number} maxAttempts Default try count is 2.
   */
  async select(option: string, opt: { waitForNav?: boolean } = {}): Promise<void> {
    await this.open();
    await super.select(option, opt);
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
  private async open(maxAttempts = 2): Promise<void> {
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
