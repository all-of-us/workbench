import { Page } from 'puppeteer';
import BasePage from 'app/page/base-page';
import { getPropValue } from 'utils/element-utils';
import HelpTipsSidebar from 'app/component/help-tips-sidebar';

const signedInIndicator = 'app-signed-in';

/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {
  protected constructor(page: Page) {
    super(page);
  }

  protected async isSignedIn(): Promise<boolean> {
    return this.page
      .waitForSelector(signedInIndicator, { timeout: 3 * 60 * 1000 })
      .then((elemt) => elemt.asElement() !== null);
  }

  /**
   * Method to be implemented by children classes.
   * Check whether current page has specified web elements.
   */
  abstract isLoaded(): Promise<boolean>;

  /**
   * Wait until current page is loaded and without spinners spinning.
   */
  async waitForLoad(): Promise<this> {
    await this.isSignedIn();
    await this.isLoaded();
    await this.closeHelpSidebarIfOpen();
    return this;
  }

  /**
   * Load AoU page URL.
   */
  async loadPageUrl(url: string): Promise<void> {
    await this.gotoUrl(url);
    await this.waitForLoad();
  }

  /**
   * Find the actual displayed User name string in sidenav dropdown.
   */
  async getUsername(): Promise<string> {
    const xpath = '//*[child::clr-icon[@shape="angle"]/*[@role="img"]]';
    const username = (await this.page.$x(xpath))[0];
    return getPropValue<string>(username, 'innerText');
  }

  async closeHelpSidebarIfOpen(): Promise<void> {
    const sidebar = new HelpTipsSidebar(this.page);
    const isOpen = await sidebar.isVisible();
    if (isOpen) {
      await sidebar.close();
    }
  }
}
