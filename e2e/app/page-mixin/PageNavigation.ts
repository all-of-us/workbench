import {Page} from 'puppeteer';
import {iconXpath} from '../aou-elements/xpath-defaults';
import {findIcon} from '../aou-elements/xpath-finder';


export const LINK = {
  HOME: 'Home',
  ADMIN: 'Admin',
  USER_ADMIN: 'User Admin',
  PROFILE: 'Profile',
  SIGN_OUT: 'Sign Out',
  CONTACT_US: 'Contact Us',
  USER_SUPPORT: 'User Support',
  YOUR_WORKSPACES: 'Your Workspaces',
  FEATURED_WORKSPACES: 'Featured Workspaces',
};

export const LINK_ICON = {
  HOME: 'home',
  ADMIN: 'user',
  CONTACT_US: 'envelope',
  USER_SUPPORT: 'help',
  YOUR_WORKSPACES: 'applications',
  FEATURED_WORKSPACES: 'star',
};


export default class PageNavigation {

  /**
   * Is nav menu dropdown open or closed?
   * @param page
   */
  public static async isOpen(page: Page): Promise<boolean> {
    try {
      // look for Home icon. If exception is thrown, means not found, dropdown is not open.
      await findIcon(page, 'Home', 'home', {visible: true, timeout: 1000});
      return true;
    } catch(err) {
      return false;
    }
  }

   /**
    * Open dropdown.
    */
  public static async openDropdown(page: Page) {
    const is = await PageNavigation.isOpen(page);
    if (!is) {
      // click bars icon to open dropdown
      const icon = await findIcon(page, '', 'bars');
      await icon.click();
      await page.waitForXPath(PageNavigation.angleIconXpath, {timeout: 2000});
    }
  }

  /**
   * Go to application page.
   * @param page
   * @param app
   */
  public static async goTo(page: Page, app: string) {
    await PageNavigation.openDropdown(page);
    const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${app}")]`;
    const [applink] = await page.$x(appLinkXpath);
    if (!applink) {
      const [username, admin] = await page.$x(PageNavigation.angleIconXpath);
      if (app === 'Profile' || app === 'Sign Out') {
        // Open User submenu if needed
        if (!applink) {
          await username.click();
        }
      } else if (app === 'User Admin') {
        // Open Admin submenu if needed
        if (!applink) {
          await admin.click();
        }
      }
    }
    const link = await page.waitForXPath(appLinkXpath, {timeout: 2000});
    await link.click();
    await page.waitFor(1000);
  }

  public static async getUserName(page: Page): Promise<unknown> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await page.$x(xpath))[0];
    const p = await username.getProperty('innerText');
    const value = await p.jsonValue();
    return value;
  }

  private static angleIconXpath = iconXpath('', 'angle');

}
