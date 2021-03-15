import {Page} from 'puppeteer';

export const waitForFn = async (fn: () => any, interval = 2000, timeout = 10000): Promise<boolean> => {
  const start = Date.now()
  while (Date.now() < start + timeout) {
    if (fn()) {
      return true;
    }
    await new Promise(resolve => setTimeout(resolve, interval));
  }
  return false;
}

/**
 * Wait for the window location to match sub-string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} urlSubstr - The URL partial string.
 */
export async function waitForUrl(page: Page, urlSubstr: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(txt => {
      const href = window.location.href;
      return href.includes(txt);
    }, {}, urlSubstr);
    return (await jsHandle.jsonValue()) as boolean;
  } catch (e) {
    console.error(`waitForUrl contains "${urlSubstr}" failed. ${e}`);
    throw e;
  }
}

/**
 * Wait for the document title to match string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} titleSubstr The Document title partial string.
 */
export async function waitForDocumentTitle(page: Page, titleSubstr: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(t => {
      const actualTitle = document.title;
      return actualTitle.includes(t);
    }, {timeout: 10 * 60 * 1000}, titleSubstr);
    return (await jsHandle.jsonValue()) as boolean;
  } catch (e) {
    console.error(`waitForDocumentTitle contains "${titleSubstr}" failed. ${e}`);
    throw e;
  }
}


// ************************************************************************
/**
 * Helper functions when directly dealing with XPath selectors.
 */

export async function waitForPropertyEquality(page: Page,
                                              xpathSelector: string,
                                              propertyName: string,
                                              propertyValue: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction((xpath, prop, value) => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return element[prop] === value;
    }, {}, xpathSelector, propertyName, propertyValue);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Wait for element matching XPath="${xpathSelector}" property:${propertyName} value:${propertyValue} failed. ${e}`);
    throw e;
  }
}

export async function waitForNumericalString(page: Page, xpath: string, timeout?: number): Promise<string> {
  await page.waitForXPath(xpath, {visible: true, timeout});
  const numbers =  await page.waitForFunction( xpathSelector => {
    const node = document.evaluate(xpathSelector, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    if (node !== null) {
      const txt = node.textContent.trim();
      const re = new RegExp(/\d{1,3}(,?\d{3})*/);
      if (re.test(txt)) { // Match only numbers with comma
        return re.exec(txt)[0];
      }
    }
    return false;
  }, {timeout}, xpath);

  return (await numbers.jsonValue()).toString();
}

export async function waitForPropertyNotExists(page: Page,
                                            xpathSelector: string,
                                            propertyName: string): Promise<boolean> {
  try {
    await page.waitForFunction((xpath, prop) => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return !element[prop];
    }, {}, xpathSelector, propertyName);
    return true;
  } catch (e) {
    console.error(`Failed waiting element (XPath="${xpathSelector}") property: ${propertyName} not exists. ${e}`);
    throw e;
  }
}

export async function waitForPropertyExists(page: Page, xpathSelector: string, propertyName: string): Promise<boolean> {
  try {
    await page.waitForFunction((xpath, prop) => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return element[prop] !== null;
    }, {}, xpathSelector, propertyName);
    return true;
  } catch (err) {
    console.error(`Failed waiting element (XPath="${xpathSelector}") property: ${propertyName} exists. ${err}`);
    throw (err);
  }
}

// ************************************************************************
/**
 * Helper functions when directly dealing with CSS selectors.
 */

/**
 * Wait until the selector is visible.
 * @param {Page} page The Puppeteer Page.
 * @param {string} cssSelector The CSS selector.
 */
export async function waitForVisible(page: Page, cssSelector: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !!isVisible;
    }, {}, cssSelector);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`waitForVisible CSS="${cssSelector}" failed. ${e}`);
    throw e;
  }
}

/**
 * Wait while the selector is visible. Stops waiting when no longer has visible contents.
 * @param {Page} page The Puppeteer Page
 * @param {string} cssSelector The CSS selector
 */
export async function waitForHidden(page: Page, cssSelector: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !isVisible;
    }, {}, cssSelector);
    return (await jsHandle.jsonValue()) as boolean;
  } catch (e) {
    console.error(`waitForHidden CSS="${cssSelector}" failed. ${e}`);
    throw e;
  }
}

/**
 * Wait for the element found from the selector has a particular attribute value pair
 * @param {string} cssSelector - The selector for the element on the page
 * @param {string} attribute - The attribute name
 * @param {string} value - The attribute value to match
 * @param {number} timeout - the timeout in msecs
 */
export async function waitForAttributeEquality(page: Page,
                                               selector: {xpath?: string, css?: string},
                                               attribute: string,
                                               value: string,
                                               timeout?: number): Promise<boolean> {
  if (selector.css !== undefined) {
    try {
      const jsHandle = await page.waitForFunction((css, attributeName, attributeValue) => {
        const element = document.querySelector(css);
        if (element != null) {
          return element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
        }
        return false;
      }, {timeout: timeout || 30000}, selector.css, attribute, value);
      return (await jsHandle.jsonValue()) as boolean;
    } catch (e) {
      console.error(`Find element matching CSS=${selector.css} attribute=${attribute} value=${value}. ${e}`);
      throw new Error(e);
    }
  }
  if (selector.xpath !== undefined) {
    try {
      const jsHandle = await page.waitForFunction((xpath, attributeName, attributeValue) => {
        const element: any = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        return element && element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
      }, {timeout: timeout || 30000}, selector.xpath, attribute, value);
      return (await jsHandle.jsonValue()) as boolean;
    } catch (e) {
      console.error(`Find element matching Xpath=${selector.xpath} attribute=${attribute} value=${value}. ${e}`);
      throw new Error(e);
    }
  }
  throw new Error('Required selector: {xpath or css} is missing');
}

/**
 * Wait for the elements count to be a particular value
 * @param {string} cssSelector - The css selector for the element
 * @param {number} expectedCount - The expected number of elements
 */
export async function waitForNumberElements(page: Page,
                                            cssSelector: string,
                                            expectedCount: number): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length === count;
    }, {}, cssSelector, expectedCount);
    return (await jsHandle.jsonValue()) as boolean;
  } catch (e) {
    console.error(`Wait for elements matching CSS="${cssSelector}" count=${expectedCount} failed. ${e}`);
    throw e;
  }
}

/**
 * Wait for visible texts to match.
 * @param page
 * @param textSubstr
 * @param selector: {css, xpath}
 */
export async function waitForText(page: Page,
                                  textSubstr: string,
                                  selector: {xpath?: string, css?: string} = {css: 'body'},
                                  timeout?: number): Promise<boolean> {
  if (selector.css !== undefined) {
    try {
      // wait for visible then compare texts
      await page.waitForSelector(selector.css, {visible: true, timeout});
      const jsHandle = await page.waitForFunction((css, expText) => {
        const element = document.querySelector(css);
        const regExp = new RegExp(expText);
        return element != null && regExp.test(element.textContent);
      }, {timeout}, selector.css, textSubstr);
      return (await jsHandle.jsonValue()) as boolean;
    } catch (e) {
      console.error(`Wait for element matching CSS=${selector.css} contains "${textSubstr}" text failed. ${e}`);
      throw e;
    }
  }
  if (selector.xpath !== undefined) {
    try {
      await page.waitForXPath(selector.xpath, {visible: true, timeout});
      const jsHandle = await page.waitForFunction((xpath, expText) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        const regExp = new RegExp(expText);
        return element != null && regExp.test(element.textContent);
      }, {timeout}, selector.xpath, textSubstr);
      return (await jsHandle.jsonValue()) as boolean;
    } catch (e) {
      console.error(`Wait for element matching Xpath=${selector.xpath} contains "${textSubstr}" text failed. ${e}`);
      throw e;
    }
  }
  throw new Error('xpath or css is required');
}

/**
 * <pre>
 * Wait while the page is loading (spinner is spinning and visible). Waiting stops when spinner stops spinning or when timed out.
 * It usually indicates the page is ready for user interaction.
 * </pre>
 */
export async function waitWhileLoading(page: Page, timeout: number = (2 * 60 * 1000), opts: {waitForRuntime?: boolean} = {}): Promise<void> {
  const {waitForRuntime = false}  = opts;

  const notBlankPageSelector = '[data-test-id="sign-in-container"], title:not(empty), div.spinner, svg[viewBox]';
  const spinElementsSelector = `[style*="running spin"], .spinner:empty, [style*="running rotation"]${waitForRuntime ? '' : ':not([aria-hidden="true"])'}`;

  // To prevent checking on blank page, wait for elements exist in DOM.
  await Promise.race([
    page.waitForSelector(notBlankPageSelector),
    page.waitForSelector(spinElementsSelector),
  ]);

  // Wait for spinners stop and gone.
  await page.waitForFunction((css) => {
    const elements = document.querySelectorAll(css);
    return elements && elements.length === 0;
  }, {polling: 'mutation', timeout}, spinElementsSelector);

  await page.waitForTimeout(500);
}

export async function waitUntilEnabled(page: Page, cssSelector: string): Promise<boolean> {
  const jsHandle = await page.waitForFunction(xpathSelector => {
    const elemt = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    const style = window.getComputedStyle(elemt as Element);
    const propValue = style.getPropertyValue('cursor');
    return propValue === 'pointer';
  }, {}, cssSelector);
  return (await jsHandle.jsonValue()) as boolean;
}
