import {Page} from 'puppeteer';
import {PageUrl} from 'app/page-identifiers';
import BasePage from 'app/page/base-page';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';

const SELECTOR = {
  signedInIndicator: 'app-signed-in',
};


/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {

  protected constructor(page: Page) {
    super(page);
  }

  protected async isSignedIn(): Promise<boolean> {
    try {
      await this.page.waitForSelector(SELECTOR.signedInIndicator);
      return true;
    } catch (err) {
      console.log(`AuthenticatedPage isSignedIn() encountered ${err}`);
      return false;
    }
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
    const isSignedIn = await this.isSignedIn();
    const isPageLoaded = await this.isLoaded();
    if (!isSignedIn || !isPageLoaded) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      const title = await this.page.title();
      throw new Error(`${title}: page not loaded correctly.`);
    }
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
   * Find the actual displayed User name string in sidenav dropdown.
   */
  async getUsername(): Promise<unknown> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await this.page.$x(xpath))[0];
    const p = await username.getProperty('innerText');
    const value = await p.jsonValue();
    return value;
  }


}
