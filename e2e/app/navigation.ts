import {ElementHandle, Page} from 'puppeteer';
import {clrIconXpath} from 'app/aou-elements/xpath-defaults';
import {findIcon} from 'app/aou-elements/xpath-finder';

export enum NavLink {
  HOME = 'Home',
  ADMIN = 'Admin',
  USER_ADMIN = 'User Admin',
  PROFILE = 'Profile',
  SIGN_OUT = 'Sign Out',
  CONTACT_US = 'Contact Us',
  USER_SUPPORT = 'User Support',
  YOUR_WORKSPACES = 'Your Workspaces',
  FEATURED_WORKSPACES = 'Featured Workspaces',
}

export enum NavLinkIcon {
  HOME = 'home',
  ADMIN = 'user',
  CONTACT_US = 'envelope',
  USER_SUPPORT = 'help',
  YOUR_WORKSPACES = 'applications',
  FEATURED_WORKSPACES = 'star',
}

export default class Navigation {

  /**
   * Using SideNav menu, open another application page.
   * @param {Page} page
   * @param {NavLink} destinationApp
   */
  static async navMenu(page: Page, destinationApp: NavLink) {

    const findMenuItem = async (): Promise<ElementHandle | null> => {
      await Navigation.openNavMenu(page);
      const angleIconXpath = clrIconXpath({}, 'angle');
      await page.waitForXPath(angleIconXpath, {timeout: 2000});
      const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${destinationApp}")]`;
         // Find link. If not found, determine if link should be found in a submenu: User or Admin
      const [applink] = await page.$x(appLinkXpath);
      if (!applink) {
            // If it's a link under User submenu.
        const [username, admin] = await page.$x(angleIconXpath);
        if (destinationApp === NavLink.PROFILE || destinationApp === NavLink.SIGN_OUT) {
               // Open User submenu if needed
          if (!applink) {
            await username.click();
            return page.waitForXPath(appLinkXpath, {timeout: 2000});
          }
        } else if (destinationApp === NavLink.USER_ADMIN) {
               // If it's a link under Admin submenu, open Admin submenu if needed
          if (!applink) {
            await admin.click();
            return page.waitForXPath(appLinkXpath, {timeout: 2000});
          }
        }
      } else {
        return applink;
      }
    }

    // find target sidenav link. If not found, return null.
    const link = await findMenuItem();
    if (!link) {
      return null;
    }
    if (destinationApp === NavLink.CONTACT_US) {
      await link.click();
    } else {
         // click and wait for page navigation
      return Promise.all([
        page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 90000}),
        link.click(),
      ]);
    }
  }

  /**
   * Open SideNav menu.
   * @param page
   */
  static async openNavMenu(page: Page): Promise<void> {
    const isOpen = await Navigation.sideNavIsOpen(page);
    if (!isOpen!) {
        // click bars icon to open dropdown
      const barsIcon = await findIcon(page, {}, 'bars');
      await barsIcon.click();
    }
  }

  // Determine the open state by looking for a visible Home icon
  private static async sideNavIsOpen(page: Page): Promise<boolean> {
    try {
      await findIcon(page, {text: 'Home'}, 'home', {visible: true, timeout: 1000});
      return true;
    } catch (err) {
      return false;
    }
  }

}
