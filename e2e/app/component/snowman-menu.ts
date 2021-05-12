import { Page } from 'puppeteer';
import { MenuOption } from 'app/text-labels';
import Link from 'app/element/link';
import BaseMenu from './base-menu';

export const snowmanIconXpath = '//clr-icon[@shape="ellipsis-vertical"]';
const defaultXpath = '//*[@id="popup-root"]/div';

export default class SnowmanMenu extends BaseMenu {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   *  Get texts of all visible options.
   */
  async getAllOptionTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}//*[@role="button"]/text()`;
    return this.getMenuItemTexts(selector);
  }

  /**
   * Determine if an option in snowman menu is disabled.
   * @param {MenuOption} option Snowman menuitem.
   */
  async isOptionDisabled(option: MenuOption): Promise<boolean> {
    const link = this.findOptionLink(option);
    return link.isCursorNotAllowed();
  }

  getMenuItemXpath(menuItemText: string): string {
    return `//*[@role="button" and normalize-space()="${menuItemText}"]`;
  }

  private findOptionLink(option: MenuOption): Link {
    const selector = `${this.getXpath()}${this.getMenuItemXpath(option)}`;
    return new Link(this.page, selector);
  }
}
