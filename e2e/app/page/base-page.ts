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
   * Load AoU page url or reload page.
   *
   */
  async loadPage(opts: { url?: string; reload?: boolean } = {}): Promise<void> {
    const { url, reload } = opts;
    if (url !== undefined && reload !== undefined) {
      throw new Error('Invalid parameters: url and reload both defined.');
    }
    if (url === undefined && reload === undefined) {
      throw new Error('Invalid parameters: url and reload both undefined.');
    }
    const timeout = 30 * 1000;
    try {
      if (url !== undefined) {
        logger.info(`Go to page: ${url}`);
        await this.page.goto(url, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'], timeout });
      }
      if (reload !== undefined) {
        logger.info(`Reload page: ${this.page.url()}`);
        await this.page.reload({ waitUntil: ['networkidle0', 'load', 'domcontentloaded'], timeout });
      }
      await waitWhileLoading(this.page, { timeout });
    } catch (err) {
      logger.error(`ERROR: Encountered error when loading page.\n${err}`);
      // Let test continue
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
