import {ElementHandle, Page, Response} from 'puppeteer';
import { ensureDir, writeFile } from 'fs-extra';

/**
 * All Page Object classes will extends the BasePage.
 * Contains common functions/actions that help with tests creation.
 */
export default abstract class BasePage {

  page: Page;
  DEFAULT_TIMEOUT_MILLISECONDS = 30000;
  timeout;

  protected constructor(page: Page, timeout?) {
    this.page = page;
    this.timeout = timeout || this.DEFAULT_TIMEOUT_MILLISECONDS;
  }

  async getPageTitle() : Promise<string> {
    return this.page.title();
  }

  /**
   * Reload current page.
   */
  async reloadPage(): Promise<Response> {
    return await this.page.reload( { waitUntil: ['networkidle0', 'domcontentloaded'] } );
  }

  /**
   * Load a URL.
   */
  async gotoUrl(url: string): Promise<void> {
    await this.page.goto(url, {waitUntil: ['domcontentloaded','networkidle0']});
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

  async waitForNavigation() {
    return Promise.all([
      this.page.waitForNavigation({waitUntil: 'load'}),
      this.page.waitForNavigation({waitUntil: 'domcontentloaded'}),
      this.page.waitForNavigation({waitUntil: 'networkidle0', timeout: 60000}),
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

  /**
   * Find texts in DOM.
   * @param txt
   */
  async findText(txt: string): Promise<boolean> {
    const indx = await (await this.page.content()).indexOf(txt);
    return indx !== -1;
  }

  /**
   * <pre>
   * Get the value of a particular property for a particular element
   * </pre>
   * @param {ElementHandle} element - The web element to get the property value for
   * @param {string} property - The property to look for
   * @returns {string} value - The property value
   */
  async getProperty(element: ElementHandle, property: string) {
    // Alternative: return element.getProperty(property).then((elem) => elem.jsonValue());
    const handle = await this.page.evaluateHandle((elem, prop) => {
      return elem[prop];
    }, element, property);
    return await handle.jsonValue();
  }

  /**
   * Get the element attribute value.
   * @param {ElementHandle} element
   * @param {string} attribute
   */
  async getAttribute(element: ElementHandle, attribute: string) {
    const handle = await this.page.evaluateHandle((elem, attr) => {
      return elem.getAttribute(attr);
    }, element, attribute);
    return await handle.jsonValue();
  }

  /**
   * <pre>
   * Check whether a web element exist. Visibility is not checked.
   * </pre>
   * @param {ElementHandle} element: The web element to check
   */
  async exists(element: ElementHandle) {
    return await this.page.evaluate(elem => {
      return elem !== null;
    }, element);
  }

  /**
   * Take a full-page screenshot, save file in .png format in logs/screenshot directory.
   * @param fileName
   */
  async takeScreenshot(fileName: string) {
    const screenshotDir = 'logs/screenshot';
    await ensureDir(screenshotDir);
    const timestamp = new Date().getTime();
    const screenshotFile = `${screenshotDir}/${fileName}_${timestamp}.png`;
    await this.page.screenshot({path: screenshotFile, fullPage: true});
    console.log('Saved screenshot ' + screenshotFile);
  }

  async saveToFile(fileName, data, suffix: string = 'html') {
    const logDir = 'logs/html';
    await ensureDir(logDir);
    const fname = `${logDir}/${fileName}-${new Date().getTime()}.${suffix}`;
    return new Promise((resolve, reject) => {
      writeFile(fname, data, 'utf8', error => {
        if (error) {
          console.error(`save file failed. ` + error);
          reject(false);
        } else {
          console.log('Saved file ' + fname);
          resolve(true);
        }
      })
    });
  }

  /**
   * Save Html source to a file. Useful for test failure troubleshooting.
   * @param {Puppeteer.Page} page
   * @param {string} fileName
   */
  async saveHtmlToFile(page, fileName: string) {
    const html = await page.content();
    await this.saveToFile(fileName, html);
  }

}
