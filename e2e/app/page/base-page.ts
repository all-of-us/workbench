import { Page, Response } from 'puppeteer';

/**
 * All Page Object classes will extends the BasePage.
 * Contains common functions/actions that help with tests creation.
 */
export default abstract class BasePage {
  page: Page;

  protected constructor(page: Page) {
    this.page = page;
  }

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
    await this.page.goto(url, { waitUntil: ['networkidle0', 'load'], timeout: 0 });
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
