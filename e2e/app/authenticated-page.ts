import {Page} from 'puppeteer';
import {clrIconXpath} from './aou-elements/xpath-defaults';
import {findIcon} from './aou-elements/xpath-finder';
import BasePage from './base-page';

const configs = require('../resources/workbench-config');

const selectors = {
  signedInIndicator: 'body#body div',
  logo: 'img[src="/assets/images/all-of-us-logo.svg"]'
};

export enum PageUrl {
  HOME = configs.uiBaseUrl,
  WORKSPACES = configs.uiBaseUrl + configs.workspacesUrlPath,
  ADMIN = configs.uiBaseUrl + configs.adminUrlPath,
}

export enum SideNavLink {
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

export enum SideNavLinkIcon {
  HOME = 'home',
  ADMIN = 'user',
  CONTACT_US = 'envelope',
  USER_SUPPORT = 'help',
  YOUR_WORKSPACES = 'applications',
  FEATURED_WORKSPACES = 'star',
}


/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {

  protected constructor(page: Page) {
    super(page);
  }

  /**
   * Take a full-page screenshot, save file in .png format in logs/screenshots directory.
   * @param fileName
   */
  async takeScreenshot(fileName: string) {
    const timestamp = new Date().getTime();
    const screenshotFile = `screenshots/${fileName}_${timestamp}.png`;
    await this.page.screenshot({path: screenshotFile, fullPage: true});
  }

  protected async isSignedIn(): Promise<boolean> {
    await this.page.waitForSelector(selectors.signedInIndicator);
    await this.page.waitForSelector(selectors.logo, {visible: true});
    return true;
  }

  /**
   * Method to be implemented by children classes.
   * Check whether current page has specified web elements.
   */
  abstract async isLoaded(): Promise<boolean>

  /**
   * Wait until current page is loaded and without spinners spinning.
   */
  async waitForLoad(): Promise<this> {
    await this.isLoaded();
    await this.waitUntilNoSpinner();
    return this;
  }

  /**
   * Load AoU page URL.
   */
  async loadPageUrl(url: PageUrl): Promise<void> {
    await this.gotoUrl(url.toString());
    await this.waitForLoad();
  }

  /**
   * Go to application page.
   * @param targetPage
   */
  async navTo(targetPage: SideNavLink) {
    await this.openSideNav();
    const angleIconXpath = clrIconXpath('', 'angle');
    await this.page.waitForXPath(angleIconXpath, {timeout: 2000});
    const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${targetPage}")]`;
    // try to find target sidenav link. Get the first element from ElementHandle array
    const [applink] = await this.page.$x(appLinkXpath);
    if (!applink) {
      // if sidnav link is not found, check to see if it's a link under User or Admin submenu.
      const [username, admin] = await this.page.$x(angleIconXpath);
      if (targetPage === SideNavLink.PROFILE || targetPage === SideNavLink.SIGN_OUT) {
        // Open User submenu if needed
        if (!applink) {
          await username.click();
        }
      } else if (targetPage === SideNavLink.USER_ADMIN) {
        // Open Admin submenu if needed
        if (!applink) {
          await admin.click();
        }
      }
    }
    // find target sidenav link again. If not found, throws exception
    const link = await this.page.waitForXPath(appLinkXpath, {timeout: 2000});
    if (targetPage === SideNavLink.CONTACT_US) {
      await link.click();
    } else {
      const navPromise = this.waitForNavigation();
      await link.click();
      await navPromise;
    }

  }

  /**
   * Find the actual displayed User name string in sidenav dropdown.
   */
  async getUserName(): Promise<unknown> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await this.page.$x(xpath))[0];
    const p = await username.getProperty('innerText');
    const value = await p.jsonValue();
    return value;
  }

  /**
   * Open sidenav dropdown.
   */
  async openSideNav() {
    const is = await this.isSideNavDropdownOpen();
    if (!is) {
      // click bars icon to open dropdown
      const icon = await findIcon(this.page, '', 'bars');
      await icon.click();
    }
  }

  /**
   * <pre>
   * Wait until spinner stops spinning, usually it indicate page is ready.
   * </pre>
   */
  async waitUntilNoSpinner() {
    // wait maximum 1 second for either spinner to show up
    const selectr1 = '.spinner, svg';
    const spinner = await this.page.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, selectr1);
    const jValue = await spinner.jsonValue();

    // wait maximum 90 seconds for spinner disappear if spinner existed
    const selectr2 = 'svg[style*="spin"], .spinner:empty';
    try {
      if (jValue) {
        await this.page.waitFor((selector) => {
          const len = document.querySelectorAll(selector).length;
          return len === 0
        }, {timeout: 90000}, selectr2);
      }
    } catch (err) {
      await this.takeScreenshot('TimedOutWaitForSpinnerStop');
      throw err;
    }
    // final 1 second wait for page render to finish
    if (jValue) {
      await this.page.waitFor(1000);
    }
  }

  private async isSideNavDropdownOpen(): Promise<boolean> {
    try {
      await findIcon(this.page, 'Home', 'home', {visible: true, timeout: 1000});
      return true;
    } catch(err) {
      return false;
    }
  }


}
