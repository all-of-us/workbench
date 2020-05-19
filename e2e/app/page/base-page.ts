import {Page, Response} from 'puppeteer';

/**
 * All Page Object classes will extends the BasePage.
 * Contains common functions/actions that help with tests creation.
 */
export default abstract class BasePage {

  protected readonly puppeteerPage: Page;

  protected constructor(page: Page) {
    this.puppeteerPage = page;
  }

  async getPageTitle() : Promise<string> {
    return this.puppeteerPage.title();
  }

  /**
   * Reload current page.
   */
  async reloadPage(): Promise<Response> {
    return await this.puppeteerPage.reload( { waitUntil: ['networkidle0', 'domcontentloaded'] } );
  }

  /**
   * Load a URL.
   */
  async gotoUrl(url: string): Promise<void> {
    await this.puppeteerPage.goto(url, {waitUntil: ['domcontentloaded','networkidle0']});
  }

  /**
   *  <pre>
   * Get the element's textContent for a specified CSS selector.
   *  </pre>
   * @param {string} cssSelector
   * @returns {string} value - The text property value
   */
  async getTextContent(cssSelector: string) {
    return this.puppeteerPage.$eval(`${cssSelector}`, elem => elem.textContent.trim())
  }

  /**
   * Find texts in DOM.
   * @param txt
   */
  async containsText(txt: string): Promise<boolean> {
    const indx = await (await this.puppeteerPage.content()).indexOf(txt);
    return indx !== -1;
  }

}
