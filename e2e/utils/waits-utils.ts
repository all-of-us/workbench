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
 * Wait for the window location to match string.
 * @param {Page} page The Puppeteer Page
 * @param {string} urlSubstr - URL regular expression
 */
export async function waitForUrl(page: Page, urlSubstr: string, timeOut: number = 30000): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(txt => {
      const href = window.location.href;
      return href.includes(txt);
    }, {timeout: timeOut}, urlSubstr);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for page URL contains "${urlSubstr}" failed. ${e}`);
    throw e;
  }
}

/**
 * Wait for the document title to match string.
 * @param {Page} page The Puppeteer Page
 * @param {string} titleSubstr
 */
export async function waitForDocumentTitle(page: Page, titleSubstr: string, timeOut: number = 30000): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(t => {
      const actualTitle = document.title;
      return actualTitle.includes(t);
    }, {timeout: timeOut}, titleSubstr);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for page title contains "${titleSubstr}" failed. ${e}`);
    throw e;
  }
}


// ************************************************************************
/**
 * Helper functions when directly dealing with XPath selectors.
 */

export async function waitForPropertyEquality(
   page: Page,
   xpathSelector: string,
   propertyName: string,
   propertyValue: string,
   timeOut: number = 30000): Promise<boolean> {

  try {
    const jsHandle = await page.waitForFunction((xpath, prop, value) => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return element[prop] === value;
    }, {timeout: timeOut}, xpathSelector, propertyName, propertyValue);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for element matching XPath="${xpathSelector}" property:${propertyName} value:${propertyValue} failed. ${e}`);
    throw e;
  }
}

export async function waitForNumericalString(page: Page, xpath: string, timeOut: number = 30000): Promise<string> {
  await page.waitForXPath(xpath, {visible: true});

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
  }, {timeout: timeOut}, xpath);

  return (await numbers.jsonValue()).toString();
}

export async function waitForPropertyNotExists(page: Page,
                                            xpathSelector: string,
                                            propertyName: string,
                                            timeOut: number = 30000): Promise<boolean> {

  try {
    await page.waitForFunction((xpath, prop) => {
      const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      return !element[prop];
    }, {timeout: timeOut}, xpathSelector, propertyName);
    return true;
  } catch (e) {
    console.error(`Failed waiting element (XPath="${xpathSelector}") property: ${propertyName} not exists. ${e}`);
    throw e;
  }
}

// ************************************************************************
/**
 * Helper functions when directly dealing with CSS selectors.
 */

/**
 * Wait until the selector is visible.
 * @param {string} cssSelector The CSS selector
 */
export async function waitForVisible(page: Page, cssSelector: string, timeOut: number = 30000): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !!isVisible;
    }, {timeout: timeOut}, cssSelector);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for element matching CSS="${cssSelector}" visible failed. ${e}`);
    throw e;
  }
}

/**
 * Wait while the selector is visible. Stops waiting when no longer has visible contents.
 * @param {Page} page The Puppeteer Page
 * @param {string} cssSelector The CSS selector
 */
export async function waitForHidden(page: Page, cssSelector: string, timeOut: number = 30000): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(selector => {
      const elem = document.querySelector(selector);
      const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
      return !isVisible;
    }, {timeout: timeOut}, cssSelector);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for element matching CSS="${cssSelector}" hidden. ${e}`);
    throw e;
  }
}

/**
 * Wait for the element found from the selector has a particular attribute value pair
 * @param {string} cssSelector - The selector for the element on the page
 * @param {string} attribute - The attribute name
 * @param {string} value - The attribute value to match
 */
export async function waitForAttributeEquality(
   page: Page,
   selector: {xpath?: string, css?: string},
   attribute: string,
   value: string,
   timeOut: number = 30000): Promise<boolean> {

  if (selector.css !== undefined) {
    try {
      const jsHandle = await page.waitForFunction((css, attributeName, attributeValue) => {
        const element = document.querySelector(css);
        if (element != null) {
          return element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
        }
        return false;
      }, {timeout: timeOut}, selector.css, attribute, value);
      await jsHandle.jsonValue();
      return true;
    } catch (e) {
      console.error(`Waiting for element matching CSS="${selector.css}" attribute:${attribute} value:${value} failed. ${e}`);
      throw e;
    }
  } else {
    try {
      const jsHandle = await page.waitForFunction((xpath, attributeName, attributeValue) => {
        const element: any = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        return element && element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue;
      }, {timeout: timeOut}, selector.xpath, attribute, value);
      await jsHandle.jsonValue();
      return true;
    } catch (e) {
      console.error(`Waiting for element matching Xpath=${selector.xpath} attribute:${attribute} value:${value} failed. ${e}`);
      throw e;
    }
  }
}

/**
 * Wait for the elements count to be a particular value
 * @param {string} cssSelector - The css selector for the element
 * @param {number} expectedCount - The expected number of elements
 */
export async function waitForNumberElements(
   page: Page,
   cssSelector: string,
   expectedCount: number,
   timeOut: number = 30000): Promise<boolean> {

  try {
    const jsHandle = await page.waitForFunction((selector, count) => {
      return document.querySelectorAll(selector).length === count;
    }, {timeout: timeOut}, cssSelector, expectedCount);
    await jsHandle.jsonValue();
    return true;
  } catch (e) {
    console.error(`Waiting for elements matching CSS="${cssSelector}" count=${expectedCount} failed. ${e}`);
    throw e;
  }
}

/**
 * Wait for visible texts to match.
 * @param page
 * @param textSubstr
 * @param selector: {css, xpath}
 */
export async function waitForText(
   page: Page,
   textSubstr: string,
   selector: {xpath?: string, css?: string} = {css: 'body'},
   timeOut: number = 30000): Promise<boolean> {

  if (selector.css !== undefined) {
    try {
         // wait for visible then compare texts
      await page.waitForSelector(selector.css, {visible: true, timeout: timeOut});
      const jsHandle = await page.waitForFunction((css, expText) => {
        const element = document.querySelector(css);
        return element && element.textContent.includes(expText);
      }, {timeout: timeOut}, selector.css, textSubstr);
      await jsHandle.jsonValue();
      return true;
    } catch (e) {
      console.error(`Waiting for element matching CSS=${selector.css} contains "${textSubstr}" text failed. ${e}`);
      throw e;
    }
  } else {
    try {
      await page.waitForXPath(selector.xpath, {visible: true, timeout: timeOut});
      const jsHandle = await page.waitForFunction((xpath, expText) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        return element && element.textContent.includes(expText);
      }, {timeout: timeOut}, selector.xpath, textSubstr);
      await jsHandle.jsonValue();
      return true;
    } catch (e) {
      console.error(`Waiting for element matching Xpath=${selector.xpath} contains "${textSubstr}" text failed. ${e}`);
      throw e;
    }
  }


}
