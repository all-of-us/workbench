import {Page, Response} from 'puppeteer';
import {waitForNavigation} from '../driver/page-wait';
import {clrIconXpath} from './aou-elements/xpath-defaults';
import {findIcon} from './aou-elements/xpath-finder';
import BasePage from './base';

const configs = require('../resources/workbench-config.js');

const selectors = {
  signedInIndicator: 'body#body div',
  logo: 'img[src="/assets/images/all-of-us-logo.svg"]'
};

export enum AppUrl {
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
 * AoU basepage class for extending.
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

  abstract async isLoaded(): Promise<boolean>

  async waitForLoad(): Promise<this> {
    await this.isLoaded();
    await this.waitUntilNoSpinner();
    return this;
  }

  async reloadPage(): Promise<Response> {
    return await this.page.reload( { waitUntil: ['networkidle0', 'domcontentloaded'] } );
  }

  /**
   * Load URL
   */
  async loadUrl(url: AppUrl): Promise<void> {
    await this.page.goto(url.toString(), {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForLoad();
  }

  /**
   * Go to application page.
   * @param page
   * @param app
   */
  async navTo(app: SideNavLink) {
    await this.openSideNav();
    const angleIconXpath = clrIconXpath('', 'angle');
    await this.page.waitForXPath(angleIconXpath, {timeout: 2000});
    const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${app}")]`;
    const [applink] = await this.page.$x(appLinkXpath);
    if (!applink) {
      const [username, admin] = await this.page.$x(angleIconXpath);
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
    const link = await this.page.waitForXPath(appLinkXpath, {timeout: 2000});
    if (app === SideNavLink.CONTACT_US) {
      await link.click();
    } else {
      const navPromise = waitForNavigation(this.page);
      await link.click();
      await navPromise;
    }

  }

  async getUserName(): Promise<unknown> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await this.page.$x(xpath))[0];
    const p = await username.getProperty('innerText');
    const value = await p.jsonValue();
    return value;
  }

  /**
   * Open dropdown.
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
   * Wait for spinner to stop to indicated page is ready.
   * </pre>
   */
  async waitUntilNoSpinner() {
    // wait maximum 1 second for either spinner to show up
    const selectr1 = '.spinner, svg';
    const spinner = await this.page.waitFor((selector) => {
      return document.querySelectorAll(selector).length > 0
    }, {timeout: 1000}, selectr1);
    const jValue = await spinner.jsonValue();

    // wait maximum 60 seconds for spinner disappear if spinner existed
    const selectr2 = 'svg[style*="spin"], .spinner:empty';

    if (jValue) {
      await this.page.waitFor((selector) => {
        return document.querySelectorAll(selector).length === 0
      }, {timeout: 60000}, selectr2);
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
