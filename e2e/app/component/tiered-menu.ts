import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class)), " p-menu-overlay-visible")]';

export default class TieredMenu extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   * Select menu item(s).
   * @param {string[]} menuItems
   */
  async clickMenuItem(menuItems: string[]): Promise<void> {
    const menuXpath = `${this.xpath}//ul`;

    // menu dropdown must be open and visible
    await this.page.waitForXPath(menuXpath, {visible: true});

    const findLink = async (menuItemText): Promise<ElementHandle> => {
      const selector = `${menuXpath}/li[contains(concat(" ", normalize-space()), " ${menuItemText}")]`;
      const elem = await this.page.waitForXPath(selector, {visible: true});
      await elem.hover();
      await elem.focus();
      return elem;
    };

    // iterate thru array in an orderly fashion
    const num = menuItems.length;
    for (let i=0; i<num; i++) {
      const menuItem = await findLink(menuItems[i]);
      if (i >= (num - 1)) {
        await menuItem.click();
      }
      await menuItem.dispose();
    }

    await this.page.waitForXPath(menuXpath, {hidden: true});
  }


}
