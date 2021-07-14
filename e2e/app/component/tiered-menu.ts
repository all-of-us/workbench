import { Page } from 'puppeteer';
import BaseMenu from './base-menu';

const menuXpath =
  '//*[contains(concat(" ", normalize-space(@class), " "), " p-tieredmenu ")' +
  ' or contains(concat(" ", normalize-space(@class), " "), " p-menu ")]' +
  '[contains(concat(" ", normalize-space(@class), " "), " p-menu-overlay-visible ")]';

export default class TieredMenu extends BaseMenu {
  constructor(page: Page, xpath: string = menuXpath) {
    super(page, xpath);
  }

  getMenuItemXpath(menuItemText: string): string {
    return (
      '//*[not(contains(concat(" ", normalize-space(@class), " "), " menuitem-header "))]' +
      `/*[@role="menuitem" and normalize-space()="${menuItemText}"]`
    );
  }

  async getAllOptionTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}/ul/li/a[@role="menuitem"]`;
    return this.getMenuItemTexts(selector);
  }
}
