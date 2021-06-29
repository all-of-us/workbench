import { Page } from 'puppeteer';
import Container from 'app/container';
import { MenuOption } from 'app/text-labels';
import { waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import Link from 'app/element/link';

export default abstract class BaseMenu extends Container {
  protected abstract getMenuItemXpath(menuItemText: string): string;

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), { visible: true });
    await waitWhileLoading(this.page);
  }

  protected constructor(page: Page, xpath: string) {
    super(page, xpath);
  }

  /**
   * Select menuitems.
   * @param menuSelections
   * @param opt
   */
  async select(menuSelections: string | MenuOption | MenuOption[], opt: { waitForNav?: boolean } = {}): Promise<void> {
    const { waitForNav = false } = opt;

    let maxAttempts = 3;
    const click = async (menuItem: string, xpath: string, waitForNav = false): Promise<void> => {
      const menuItemLink = await this.findMenuItemLink(menuItem, xpath);
      const hasPopup = await getPropValue<string>(await menuItemLink.asElementHandle(), 'ariaHasPopup');
      if (!hasPopup || hasPopup === 'false') {
        if (waitForNav) {
          const navigationPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
          await menuItemLink.click();
          await navigationPromise;
        } else {
          await menuItemLink.click();
        }
        return;
      }
      if (maxAttempts === 0) {
        throw new Error(`submenu not found when clicking menu item: ${menuItem}`);
      }
      // Is submenu open?
      if (await this.isOpen(`${rootXpath}/ul/li`)) {
        return;
      }
      maxAttempts--;
      await click(menuItem, xpath);
    };

    let selections = [];
    // Handle case when menuSelections is not array.
    if (typeof menuSelections === 'string') {
      selections.push(menuSelections);
    } else {
      selections = [...menuSelections];
    }

    // Wait for top-level menu dropdown open.
    await this.waitUntilVisible();
    await this.page.waitForTimeout(500);

    // Iterate orderly.
    let rootXpath = this.getXpath();
    const len = selections.length;
    for (let i = 0; i < len; i++) {
      await click(selections[i], rootXpath, i === len - 1 ? waitForNav : false);
      rootXpath = `${rootXpath}/ul`; // submenu xpath
    }

    // Wait for menu close and disappear.
    await this.waitUntilClose();
    await waitWhileLoading(this.page);
  }

  /**
   *  Get all menu items texts.
   */
  protected async getMenuItemTexts(selector: string): Promise<string[]> {
    await this.page.waitForXPath(selector, { visible: true });
    const menuItemLinks = await this.page.$x(selector);
    const actionTextsArray: string[] = [];
    for (const element of menuItemLinks) {
      actionTextsArray.push(await getPropValue<string>(element, 'textContent'));
    }
    return actionTextsArray;
  }

  protected async isOpen(xpath?: string): Promise<boolean> {
    xpath = xpath || this.getXpath();
    try {
      await this.page.waitForXPath(xpath, { visible: true, timeout: 2000 });
      return true;
    } catch (err) {
      return false;
    }
  }

  // Find and hover over menuitem
  protected async findMenuItemLink(menuItemText: string, menuParentXpath: string): Promise<Link> {
    const menuItemSelector = `${menuParentXpath}${this.getMenuItemXpath(menuItemText)}`;
    const link = new Link(this.page, menuItemSelector);
    await link.focus(10000);
    await this.page.waitForTimeout(500);
    return link;
  }
}
