import {Page} from 'puppeteer';
import {PageUrl} from 'app/page-identifiers';
import BasePage from 'app/page/base-page';
import {performance} from 'perf_hooks';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';

const SELECTOR = {
  signedInIndicator: 'body#body div',
  logo: 'img[src="/assets/images/all-of-us-logo.svg"]'
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
    await this.page.waitForSelector(SELECTOR.signedInIndicator);
    await this.page.waitForSelector(SELECTOR.logo, {visible: true});
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
    if (!await this.isLoaded()) {
      await savePageToFile(this.page, 'PageIsNotLoaded');
      await takeScreenshot(this.page, 'PageIsNotLoaded');
      throw new Error('Page isLoaded() failed.');
    }
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
    const startTime = performance.now();
    try {
      if (jValue) {
        await this.page.waitFor((selector) => {
          const selectorLength = document.querySelectorAll(selector).length;
          return selectorLength === 0;
        }, {timeout: 90000}, selectr2);
      }
    } catch (err) {
      await takeScreenshot(this.page, 'TimedOutWaitForSpinnerStop');
      throw err;
    } finally {
      const finishTime = performance.now();
      const diff = Math.floor(((finishTime - startTime) / 1000) % 60);
      if (diff > 60) {
        // if timeout exceeds 60 seconds without error thrown, it tells me page loading is slower.
        console.warn(`WARNING: waitUntilNoSpinner took ${diff} seconds.`);
      }
    }
    // final 1 second wait for page render to finish
    if (jValue) {
      await this.page.waitFor(1000);
    }
  }

}
