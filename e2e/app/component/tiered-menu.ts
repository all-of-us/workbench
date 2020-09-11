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
  async clickMenuItem(menuItems: string[], waitOptions: {waitForNav?: boolean} = {}): Promise<void> {
    const { waitForNav = false } = waitOptions;
    // menu dropdown must be open and visible
    await this.page.waitForXPath(`${this.xpath}/ul`, {visible: true});

    const findLink = async (menuItemText): Promise<ElementHandle> => {
      const selector = `${this.xpath}//ul/li[contains(concat(" ", normalize-space()), " ${menuItemText}")]`;
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
        if (waitForNav) {
          await Promise.all([
            this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0']}),
            menuItem.click(),
          ]);
        } else {
          await menuItem.click();
        }
      }
      await menuItem.dispose();
    }


  }


}
