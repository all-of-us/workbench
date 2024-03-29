import { Page } from 'puppeteer';
import BasePage from 'app/page/base-page';
import { exists, getPropValue } from 'utils/element-utils';
import HelpTipsSidebar from 'app/sidebar/help-tips-sidebar';
import { logger } from 'libs/logger';

/**
 * AuthenticatedPage represents the base page for any AoU page after user has successfully logged in (aka authenticated).
 * This is the base page for all AoU pages to extends from.
 */
export default abstract class AuthenticatedPage extends BasePage {
  protected constructor(page: Page) {
    super(page);
  }

  async isSignedIn(timeout = 60 * 1000): Promise<boolean> {
    return exists(this.page, process.env.AUTHENTICATED_TEST_ID_XPATH, { timeout });
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
    const signedIn = await this.isSignedIn();
    if (!signedIn) {
      throw new Error(`Failed to find signed-in web-element. xpath="${process.env.AUTHENTICATED_TEST_ID_XPATH}"`);
    }
    await this.isLoaded();
    await this.closeHelpSidebarIfOpen();
    const pageTitle = await this.page.title();
    logger.info(`"${pageTitle}" page loaded.`);
    return this;
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
