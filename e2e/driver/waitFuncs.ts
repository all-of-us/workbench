import {Page} from 'puppeteer';


/**
 * Wait until page URL to match a regular expression.
 * @param {string} text - URL regular expression
 */
export async function waitUntilURLMatch(page: Page, text: string) {
  return await page.waitForFunction(includesText => {
    const href = window.location.href;
    return href.includes(includesText);
  }, { timeout: this.timeout}, text);
}

/**
 * Wait until document title to be a particular string.
 * @param {string} expectedTitle - The expected title of the document
 */
export async function waitUntilTitleIs(page: Page, expectedTitle: string) {
  return await page.waitForFunction(title => {
    const actualTitle = document.title;
    return actualTitle === title;
  }, {timeout: this.timeout}, expectedTitle);
}

/**
 * Wait until document title to match a regular expression.
 * @param {string} text
 */
export async function waitUntilTitleMatch(page: Page, text: string) {
  return await page.waitForFunction((includesText) => {
    const actualTitle = document.title;
    return actualTitle.match(includesText) !== undefined;
  }, {timeout: this.timeout}, text);
}

/**
 * Wait until the selector is visible.
 * @param {string} cssSelector The CSS selector
 */
export async function waitUntilVisible(page: Page, cssSelector: string) {
  return await page.waitForFunction(selector => {
    const elem = document.querySelector(selector);
    const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
    return !!isVisible;
  }, {timeout: this.timeout}, cssSelector);
}

/**
 * Wait while the selector is visible. Stops waiting when no longer has visible contents.
 * @param {string} cssSelector The CSS selector
 */
export async function waitUntilInvisible(page: Page, cssSelector: string) {
  return await page.waitForFunction(selector => {
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
export async function waitForNumberElements(page: Page, cssSelector: string, expectedCount: number) {
  return await page.waitForFunction((selector, count) => {
    return document.querySelectorAll(selector).length === count;
  }, {timeout: this.timeout}, cssSelector, expectedCount);
}

/**
 * Wait for the elements count to be greater than a particular value
 * @param {string} cssSelector - The css selector for the element
 * @param {number} greaterThanCount - The expected number of elements
 */
export async function waitForNumberElementsIsBiggerThan(page: Page, cssSelector: string, greaterThanCount: number) {
  return await page.waitForFunction((selector, count) => {
    return document.querySelectorAll(selector).length > count;
  }, {timeout: this.timeout}, cssSelector, greaterThanCount);
}

/**
 * Look for visible texts on the page.
 * @param {string} text Partial texts to look for
 */
export async function waitUntilFindTexts(page: Page, texts: string) {
  return await page.waitForFunction(matchText => {
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
export async function waitUntilContainsAttributeValue(page: Page, cssSelector, attribute, value) {
  return await page.waitForFunction((selector, attributeName, attributeValue) => {
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
export async function waitForText(page: Page, cssSelector: string, expectedText: string) {
  await page.waitForFunction( (css, expText) => {
    const t = document.querySelector(css);
    if (t !== undefined) {
      return t.innerText === expText;
    }
  }, {timeout: 50000}, cssSelector, expectedText);

  await page.waitForSelector(cssSelector, { visible: true });
}
