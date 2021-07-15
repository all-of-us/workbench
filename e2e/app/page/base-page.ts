import { Page, Response } from 'puppeteer';
import { logger } from 'libs/logger';

/**
 * All Page Object classes will extends the BasePage.
 * Contains common functions/actions that help with tests creation.
 */
export default abstract class BasePage {
  protected constructor(protected readonly page: Page) {}

  async getPageTitle(): Promise<string> {
    return this.page.title();
  }

  /**
   * Reload current page.
   */
  async reloadPage(): Promise<Response> {
    return await this.page.reload({ waitUntil: ['networkidle0', 'load'] });
  }

  /**
   * Load a URL.
   */
  async gotoUrl(url: string): Promise<void> {
    logger.info(`goto url: ${url}`);
    const response = await this.page.goto(url, { waitUntil: ['load'] });
    if (response && !response.ok()) {
      // Log response if status is not OK
      logger.info(`Goto URL: ${url}. Response status: ${response.status()}\n${await response.text()}`);
    }
  }

  /**
   *  <pre>
   * Get the element's textContent for a specified CSS selector.
   *  </pre>
   * @param {string} cssSelector
   * @returns {string} value - The text property value
   */
  async getTextContent(cssSelector: string): Promise<string> {
    return this.page.$eval(`${cssSelector}`, (elem) => elem.textContent.trim());
  }

  /**
   * Find texts in DOM.
   * @param txt
   */
  async containsText(txt: string): Promise<boolean> {
    const indx = (await this.page.content()).indexOf(txt);
    return indx !== -1;
  }
}
