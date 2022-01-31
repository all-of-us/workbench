import { Page, Response } from 'puppeteer';
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
   * @param {{ url: string, reload: boolean, timeout: number }} opts
   */
  async loadPage(opts: { url?: string; reload?: boolean; timeout?: number } = {}): Promise<void> {
    const { url, reload = false, timeout = 30 * 1000 } = opts;
    try {
      let response: Response;
      if (reload) {
        logger.info(`Reload page: ${this.page.url()}`);
        response = await this.page.reload({ waitUntil: ['networkidle0', 'load', 'domcontentloaded'], timeout });
      } else if (url) {
        logger.info(`Go to page: ${url}`);
        response = await this.page.goto(url, { waitUntil: ['load', 'domcontentloaded', 'networkidle2'], timeout });
      }
      if (response && !response.ok()) {
        logger.error(
          `ERROR: Encountered error while loading page.\nResponse: ${response.status()}\n${await response.text()}`
        );
      }
      await waitWhileLoading(this.page, { timeout });
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
