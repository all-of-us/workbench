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

  /**
   * Wait until page URL to match a regular expression.
   * @param {string} text - URL regular expression
   */
  public async waitUntilUrlMatch(text: string): Promise<JSHandle> {
    return await this.puppeteerPage.waitForFunction(includesText => {
      const href = window.location.href;
      return href.includes(includesText);
    }, { timeout: this.timeout}, text);
  }

  /**
   * Wait until document title to be a particular string.
   * @param {string} expectedTitle - The expected title of the document
   */
  public async waitUntilDocumentTitleIs(expectedTitle: string) {
    return await this.puppeteerPage.waitForFunction(title => {
      const actualTitle = document.title;
      return actualTitle === title;
    }, {timeout: this.timeout}, expectedTitle);
  }

  /**
   * Wait until document title to match a regular expression.
   * @param {string} text
   */
  public async waitUntilDocumentTitleMatch(text: string) {
    return await this.puppeteerPage.waitForFunction((includesText) => {
      const actualTitle = document.title;
      return actualTitle.match(includesText) !== undefined;
    }, {timeout: this.timeout}, text);
  }

  /**
   * Wait until the selector has visible content.
   * @param {string} cssSelector The CSS selector
   */
  public async waitUntilHasVisibleContent(cssSelector: string) {
    return await this.puppeteerPage.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !!isVisible;
    }, {timeout: this.timeout}, cssSelector);
  }

  /**
   * Wait while the selector has visible content. Stops waiting when no longer has visible contents.
   * @param {string} cssSelector The CSS selector
   */
  public async waitWhileHasVisibleContent(cssSelector: string) {
    return await this.puppeteerPage.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !isVisible;
    }, {timeout: this.timeout}, cssSelector);
  }

  /**
   * Wait for the elements count to be a particular value
   * @param {string} cssSelector - The css selector for the element
   * @param {number} expectedCount - The expected number of elements
   */
  public async waitForNumberElement(cssSelector: string, expectedCount: number) {
    return await this.puppeteerPage.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length === count;
    }, {timeout: this.timeout}, cssSelector, expectedCount);
  }

  /**
   * Wait for the elements count to be greater than a particular value
   * @param {string} cssSelector - The css selector for the element
   * @param {number} greaterThanCount - The expected number of elements
   */
  public async waitForNumberElementIsBiggerThan(cssSelector: string, greaterThanCount: number) {
    return await this.puppeteerPage.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length > count;
    }, {timeout: this.timeout}, cssSelector, greaterThanCount);
  }

  /**
   * Look for visible texts on the page.
   * @param {string} text Partial texts to look for
   */
  public async waitForTextFound(text: string) {
    return await this.puppeteerPage.waitForFunction(matchText => {
      const bodyText = document.querySelector('body').innerText;
      return bodyText.match(matchText)
    }, {timeout: this.timeout}, text);
  }

  /**
   * Wait for the element found from the selector has a particular attribute value pair
   * @param {string} cssSelector - The selector for the element on the page
   * @param {string} attribute - The attribute name
   * @param {string} value - The attribute value to match
   */
  public async waitForAttribute(cssSelector, attribute, value) {
    return await this.puppeteerPage.waitForFunction((selector, attributeName, attributeValue) => {
      const element = document.querySelector(selector);
      return element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
    }, {timeout: this.timeout}, cssSelector, attribute, value);
  }

}
