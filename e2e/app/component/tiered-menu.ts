import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/waits-utils';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " p-menu-overlay-visible ")]';

// Cascading menus
export default class TieredMenu extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   * Select options in menu.
   * @param {string[]} options
   */
  async select(options: string[]): Promise<void> {
    const menuXpath = `${this.xpath}//ul`;

    // Make sure menu dropdown is open and visible.
    await this.page.waitForXPath(menuXpath, {visible: true});

    const findLink = async (menuItemText): Promise<ElementHandle> => {
      const selector = `${menuXpath}/li[contains(concat(" ", normalize-space()), " ${menuItemText}")]`;
      const elem = await this.page.waitForXPath(selector, {visible: true});
      await elem.hover();
      await elem.focus();
      return elem;
    };

    // Iterate thru array in an orderly fashion. Hover over menu option. Click on last menu option.
    const num = options.length;
    for (let i=0; i<num; i++) {
      const link = await findLink(options[i]);
      if (i >= (num - 1)) {
        await link.click();
        await waitWhileLoading(this.page);
      }
      await link.dispose();
    }

    await this.page.waitForXPath(menuXpath, {hidden: true});
  }


}
