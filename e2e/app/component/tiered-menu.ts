import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class)), " p-tieredmenu") ' +
   'and contains(concat(" ", normalize-space(@class)), " p-menu-overlay-visible")]';

export default class TieredMenu extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   * Select menu item(s).
   * @param {ElementHandle} menuParentElement
   * @param {string | string[]} selectMenuItems
   */
  async select(selectMenuItems: string[]): Promise<void> {
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
    const num = selectMenuItems.length;
    for (let i=0; i<num; i++) {
      const menuItem = await findLink(selectMenuItems[i]);
      if (i >= (num - 1)) {
        await menuItem.click();
      }
    }

  }


}
