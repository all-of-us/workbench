import { Page } from 'puppeteer';
import { logger } from 'libs/logger';
import { waitWhileLoading } from 'utils/waits-utils';

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
  async reloadPage(): Promise<void> {
    logger.info(`Reload page URL: ${this.page.url()}`);
    try {
      const response = await this.page.reload({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
      if (response && !response.ok()) {
        logger.error(`Reload page: Response status: ${response.status()}\n${await response.text()}`);
      }
      await waitWhileLoading(this.page);
    } catch (err) {
      logger.error(err);
    }
  }

  /**
   * Go to a URL.
   */
  async gotoUrl(url: string): Promise<void> {
    logger.info(`Goto URL: ${url}`);
    try {
      const response = await this.page.goto(url, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
      if (response && !response.ok()) {
        logger.error(`Goto URL: ${url}. Response status: ${response.status()}\n${await response.text()}`);
      }
      await waitWhileLoading(this.page);
    } catch (err) {
      logger.error(err);
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
