import {Page} from 'puppeteer';
import {PageUrl} from 'app/text-labels';
import BasePage from 'app/page/base-page';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import {getPropValue} from 'utils/element-utils';

const signedInIndicator = 'app-signed-in';


/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {

  constructor(page: Page) {
    super(page);
  }

  protected async isSignedIn(): Promise<boolean> {
    return this.page.waitForSelector(signedInIndicator)
      .then( (elemt) => elemt.asElement() !== null);
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
    try {
      await Promise.all([
        this.isSignedIn(),
        this.isLoaded(),
      ]);
      return this;
    } catch (err) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      throw (err);
    }
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
  async getUsername(): Promise<string> {
    const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
    const username = (await this.page.$x(xpath))[0];
    return getPropValue<string>(username, 'innerText');
  }


}
