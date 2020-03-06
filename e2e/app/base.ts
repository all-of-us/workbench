import {Page} from 'puppeteer';

export default abstract class BasePage {

  page: Page;
  DEFAULT_TIMEOUT_MILLISECONDS = 60000;
  timeout;

  protected constructor(page: Page, timeout?) {
    this.page = page;
    this.timeout = timeout || this.DEFAULT_TIMEOUT_MILLISECONDS;
  }

  async getPageTitle() : Promise<string> {
    return this.page.title();
  }

  /**
   *  <pre>
   * Get the element's textContent for a specified CSS selector.
   *  </pre>
   * @param {string} cssSelector
   * @returns {string} value - The text property value
   */
  async getTextContent(cssSelector: string) {
    return this.page.$eval(`${cssSelector}`, elem => elem.textContent.trim())
  }

  async waitForNavigation(page: Page) {
    return Promise.all([
      page.waitForNavigation({waitUntil: 'load'}),
      page.waitForNavigation({waitUntil: 'domcontentloaded'})
    ]);
  }

  /**
   * Wait until page URL to match a regular expression.
   * @param {string} text - URL regular expression
   */
  async waitUntilUrlMatch(text: string) {
    return await this.page.waitForFunction(txt => {
      const href = window.location.href;
      return href.includes(txt);
    }, { timeout: this.timeout}, text);
  }

  /**
   * Wait until document title to be a particular string.
   * @param {string} expectedTitle - The expected title of the document
   */
  async waitForTitle(title: string) {
    return await this.page.waitForFunction(t => {
      const actualTitle = document.title;
      return actualTitle === t;
    }, {timeout: this.timeout}, title);
  }

  /**
   * Wait until document title to match a regular expression.
   * @param {string} text
   */
  async waitUntilTitleMatch(title: string) {
    return await this.page.waitForFunction((t) => {
      const actualTitle = document.title;
      return actualTitle.includes(t);
    }, {timeout: this.timeout}, title)
    .catch ((err) => {
      console.error(`Failed match document title "${title}".`);
      throw err;
    })
  }

  /**
   * Wait until the selector is visible.
   * @param {string} cssSelector The CSS selector
   */
  async waitForVisible(cssSelector: string) {
    return await this.page.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !!isVisible;
    }, {timeout: this.timeout}, cssSelector);
  }

  /**
   * Wait while the selector is visible. Stops waiting when no longer has visible contents.
   * @param {string} cssSelector The CSS selector
   */
  async waitForHidden(cssSelector: string) {
    return await this.page.waitForFunction(selector => {
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
  async waitForNumberElements(cssSelector: string, expectedCount: number) {
    return await this.page.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length === count;
    }, {timeout: this.timeout}, cssSelector, expectedCount);
  }

  /**
   * Wait for the elements count to be greater than a particular value
   * @param {string} cssSelector - The css selector for the element
   * @param {number} greaterThanCount - The expected number of elements
   */
  async waitForNumberElementsIsBiggerThan(cssSelector: string, greaterThanCount: number) {
    return await this.page.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length > count;
    }, {timeout: this.timeout}, cssSelector, greaterThanCount);
  }

  /**
   * Look for visible texts on the page.
   * @param {string} texts Partial texts to look for
   */
  async waitForTextExists(texts: string) {
    return await this.page.waitForFunction(matchText => {
      const bodyText = document.querySelector('body').innerText;
      return bodyText.includes(matchText)
    }, {timeout: this.timeout}, texts);
  }

  /**
   * Wait for the element found from the selector has a particular attribute value pair
   * @param {string} cssSelector - The selector for the element on the page
   * @param {string} attribute - The attribute name
   * @param {string} value - The attribute value to match
   */
  async waitUntilContainsAttributeValue(cssSelector, attribute, value) {
    return await this.page.waitForFunction((selector, attributeName, attributeValue) => {
      const element = document.querySelector(selector);
      return element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
    }, {timeout: this.timeout}, cssSelector, attribute, value);
  }

  /**
   * Wait for exact text to match.
   * @param page
   * @param cssSelector
   * @param expectedText
   */
  async waitForText(cssSelector: string, expectedText: string) {
    await this.page.waitForFunction( (css, expText) => {
      const t = document.querySelector(css);
      if (t !== undefined) {
        return t.innerText === expText;
      }
    }, {timeout: 50000}, cssSelector, expectedText);

    await this.page.waitForSelector(cssSelector, { visible: true });
  }

}
