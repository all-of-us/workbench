import { buildXPath } from 'app/xpath-builders';
import { ElementHandle, Page } from 'puppeteer';
import { ElementType } from 'app/xpath-options';

export enum NavLink {
  HOME = 'Home',
  PROFILE = 'Profile',
  SIGN_OUT = 'Sign Out',
  YOUR_WORKSPACES = 'Your Workspaces',
  FEATURED_WORKSPACES = 'Featured Workspaces',
  USER_SUPPORT = 'User Support Hub',
  CONTACT_US = 'Contact Us',
  ADMIN = 'Admin',
  USER_ADMIN = 'User Admin',
  USER_AUDIT = 'User Audit',
  WORKSPACE_ADMIN = 'Workspaces',
  INSTITUTION_ADMIN = 'Institution Admin'
}

export enum NavLinkIcon {
  HOME = 'home',
  ADMIN = 'user',
  CONTACT_US = 'envelope',
  USER_SUPPORT = 'help',
  YOUR_WORKSPACES = 'applications',
  FEATURED_WORKSPACES = 'star'
}

export default class Navigation {
  /**
   * Using SideNav menu, open another application page.
   * @param {Page} page
   * @param {NavLink} destinationApp
   */
  static async navMenu(page: Page, destinationApp: NavLink): Promise<void> {
    const findMenuItem = async (): Promise<ElementHandle | null> => {
      await Navigation.openNavMenu(page);
      const angleIconXpath = buildXPath({ type: ElementType.Icon, iconShape: 'angle' });
      await page.waitForXPath(angleIconXpath, { timeout: 2000, visible: true });
      const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[(text()="${destinationApp}")]`;
      // Find link. If not found, determine if link should be found in a) submenu: User or Admin
      const [applink] = await page.$x(appLinkXpath);
      if (!applink) {
        // If it's a link under User submenu.
        const [username, admin] = await page.$x(angleIconXpath);
        const UserSubmenuItems = [NavLink.PROFILE, NavLink.SIGN_OUT];
        const AdminSubmenuItems = [
          NavLink.USER_ADMIN,
          NavLink.USER_AUDIT,
          NavLink.WORKSPACE_ADMIN,
          NavLink.INSTITUTION_ADMIN
        ];
        if (UserSubmenuItems.includes(destinationApp)) {
          // Open User submenu if needed
          if (!applink) {
            await username.click();
            return page.waitForXPath(appLinkXpath, { timeout: 2000, visible: true });
          }
        } else if (AdminSubmenuItems.includes(destinationApp)) {
          // If it's a link under Admin submenu, open Admin submenu if needed
          if (!applink) {
            await admin.click();
            return page.waitForXPath(appLinkXpath, { timeout: 2000, visible: true });
          }
        }
      } else {
        return applink;
      }
      return null;
    };

    // find target sidenav link. If not found, return null.
    const link = await findMenuItem();
    if (!link) {
      throw new Error(`Failed finding menuitem ${destinationApp}`);
    }
    if (destinationApp === NavLink.CONTACT_US) {
      await link.click();
    } else {
      // click and wait for page navigation
      await Promise.all([
        page.waitForNavigation({ waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 90000 }),
        link.click()
      ]);
    }
  }

  /**
   * Open SideNav menu.
   * @param page
   */
  static async openNavMenu(page: Page): Promise<void> {
    const isOpen = await Navigation.sideNavIsOpen(page);
    if (!isOpen) {
      // click bars icon to open dropdown
      const selector = buildXPath({ type: ElementType.Icon, iconShape: 'bars' });
      const barsIcon = await page.waitForXPath(selector, { visible: true });
      await barsIcon.click();
    }
  }

  // Determine the open state by looking for a visible Home icon
  private static async sideNavIsOpen(page: Page): Promise<boolean> {
    try {
      const selector = buildXPath({ name: 'Home', type: ElementType.Icon, iconShape: 'home' });
      await page.waitForXPath(selector, { visible: true, timeout: 2000 });
      return true;
    } catch (err) {
      return false;
    }
  }
}
