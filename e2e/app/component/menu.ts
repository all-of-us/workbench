import {ElementHandle} from 'puppeteer';
import Container from './container';

export default abstract class Menu extends Container {

  protected static async selectMenuHelper(parentNode: ElementHandle, menuItemSelections: string | string[]): Promise<void> {
    const uls = await parentNode.$x('.//ul');
    let menuItem = (await uls[0].$x(`./li[normalize-space(text())="${menuItemSelections}"]`))[0];
    
    // handle case of selecting single menuItem
    if (typeof menuItemSelections === 'string') {
      return menuItem.click();
    } else {
      await menuItem.hover(); // hover over menuitem should open sub-menu
    }

    // handle case of selecting one or more menuitems (like in a tiered menu)
    const menuItemSize = menuItemSelections.length;
    for (let i=1; i<menuItemSize; i++) {
      const subMenu = (await menuItem.$x('./ul'))[0];
      menuItem = (await (subMenu.$x(`./li[normalize-space(text())="${menuItemSelections[i]}"]`)))[0];
      if (i === menuItemSize - 1 ) {
        // if it is the last menu item, click on it instead hover over
        return menuItem.click();
      } else {
            // don't click on element if it's not the last element because click action closes menu dropdown
        await menuItem.hover();
      }
    }

  }

}
