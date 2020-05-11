import {ElementHandle, Page} from 'puppeteer';
import Container from './container';

export default abstract class Menu extends Container {

  protected static async selectMenuHelper(parentNode: Page | ElementHandle, menuItem: string | string[]): Promise<void> {
      // In case there are more than one "ul" elements, first "ul" element is always the top level menu
    const topLevelMenuElement = (await parentNode.$x('.//ul'))[0];
    if (typeof menuItem === 'string') {
      const elemtsArray = await topLevelMenuElement.$x(`./li[normalize-space(text())="${menuItem}"]`);
      await elemtsArray[0].click();
    }
    const menuItemLength = menuItem.length;
      // menuItemText[0] is the top level menuItem
    for (let i=0; i<menuItemLength; i++) {
      const elemt = (await (topLevelMenuElement.$x(`.//li[normalize-space(text())="${menuItem[i]}"]`)))[0];
      if (i === menuItemLength - 1 ) {
            // click on last element
        await elemt.click();
      } else {
            // don't click on element if it's not the last element because click action closes menu dropdown
        await elemt.hover();
      }
    }

  }

}
