import Container from 'app/container';
import {Option} from 'app/text-labels';
import {ElementHandle, Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/waits-utils';
import {getPropValue} from 'utils/element-utils';

export default abstract class BaseMenu extends Container {

   protected abstract getMenuItemXpath(menuItemText: string): string;

   protected constructor(page: Page, xpath: string) {
      super(page, xpath);
   }

   /**
    * Select menuitems.
    * @param menuSelections
    * @param opt
    */
   async select(menuSelections: Option | Option[], opt: { waitForNavi?: boolean } = {}): Promise<void> {
      const { waitForNavi = false } = opt;

      let selections = [];
      // Handle case when menuSelections is not array.
      if (typeof menuSelections === 'string') {
         selections.push(menuSelections);
      } else {
         selections = [...menuSelections];
      }

      // Wait for top-level menu dropdown open and has visible menu items.
      await this.waitUntilVisible();

      // Iterate orderly.
      let rootXpath = this.getXpath();
      const len = selections.length;
      for (let i=0; i<len; i++) {
         const menuItemLink = await this.findMenuItemLink(selections[i], rootXpath);
         if (i === (len - 1)) {
            // If it is the last menu item, click on it.
            if (waitForNavi) {
               await Promise.all([
                  this.page.waitForNavigation({waitUntil: ['load', 'networkidle0']}),
                  menuItemLink.click()
               ]);
            } else {
               await menuItemLink.click();
            }
         }
         rootXpath = `${rootXpath}/ul`; // submenu xpath
      }

      // Wait for menu close and disappear.
      await waitWhileLoading(this.page);
   }

   /**
    *  Get all menu items texts.
    */
   protected async getMenuItemTexts(selector: string): Promise<string[]> {
      await this.page.waitForXPath(selector, {visible: true});
      const menuItemLinks = await this.page.$x(selector);
      const actionTextsArray = [];
      for (const element of menuItemLinks) {
         actionTextsArray.push(await getPropValue<string>(element, 'textContent'));
      }
      return actionTextsArray;
   }

   protected async isOpen(): Promise<boolean> {
      try {
         await this.page.waitForXPath(this.getXpath(), {visible: true, timeout: 2000});
         return true;
      } catch (err) {
         return false;
      }
   }

   // Find and hover over menu item
   protected async findMenuItemLink(menuItemText: string, menuParentXpath: string): Promise<ElementHandle> {
      const menuItemSelector = `${menuParentXpath}${this.getMenuItemXpath(menuItemText)}`;
      const link = await this.page.waitForXPath(menuItemSelector, {visible: true});
      await link.hover();
      await link.focus();
      await this.page.waitForTimeout(500);
      return link;
   }

}
