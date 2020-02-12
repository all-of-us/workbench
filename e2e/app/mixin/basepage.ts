import {JSHandle, Page} from 'puppeteer';

export default class BasePage {

  public puppeteerPage: Page;
  public DEFAULT_TIMEOUT_MILLISECONDS = 45000;
  public timeout;

  constructor(page: Page, timeout?) {
    this.puppeteerPage = page;
    this.timeout = timeout || this.DEFAULT_TIMEOUT_MILLISECONDS;
  }

  public async getPageTitle() : Promise<string> {
    return await this.puppeteerPage.title();
  }

  /**
   *  <pre>
   * Get the element's textContent for a specified CSS selector.
   *  </pre>
   * @param {string} cssSelector
   * @returns {string} value - The text property value
   */
  public async getTextContent(cssSelector: string) {
    return await this.puppeteerPage.$eval(`${cssSelector}`, elem => elem.textContent.trim())
  }

}
