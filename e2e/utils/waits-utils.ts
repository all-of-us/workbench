import { JSHandle, Page } from 'puppeteer';

export const waitForFn = async (fn: () => any, interval = 2000, timeout = 10000): Promise<boolean> => {
  const start = Date.now();
  while (Date.now() < start + timeout) {
    if (fn()) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
  return false;
};

/**
 * Wait for the window location to match sub-string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} urlSubstr - The URL partial string.
 */
export async function waitForUrl(page: Page, urlSubstr: string): Promise<boolean> {
  const handle: JSHandle = await page.waitForFunction(
    (txt) => {
      const href = window.location.href;
      return href && href.includes(txt);
    },
    {},
    urlSubstr
  );
  return (await handle.jsonValue()) as boolean;
}

/**
 * Wait for the document title to match string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} titleSubstr The Document title partial string.
 */
export async function waitForDocumentTitle(page: Page, titleSubstr: string): Promise<boolean> {
  const handle = await page.waitForFunction(
    (t) => {
      const actualTitle = document.title;
      return actualTitle.includes(t);
    },
    { timeout: 10 * 60 * 1000 },
    titleSubstr
  );
  return (await handle.jsonValue()) as boolean;
}

// ************************************************************************
/**
 * Helper functions when directly dealing with XPath selectors.
 */

export async function waitForNumericalString(page: Page, xpath: string, timeout?: number): Promise<string> {
  await page.waitForXPath(xpath, { visible: true, timeout });
  const numbers = await page.waitForFunction(
    (xpathSelector) => {
      const node: Node = document.evaluate(
        xpathSelector,
        document.body,
        null,
        XPathResult.FIRST_ORDERED_NODE_TYPE,
        null
      ).singleNodeValue;
      if (node !== null) {
        const txt = node.textContent.trim();
        const re = new RegExp(/\d{1,3}(,?\d{3})*/);
        if (re.test(txt)) {
          // Match only numbers with comma
          return re.exec(txt)[0];
        }
      }
      return false;
    },
    { timeout },
    xpath
  );

  return (await numbers.jsonValue()).toString();
}

export async function waitForPropertyExists(page: Page, xpathSelector: string, propertyName: string): Promise<boolean> {
  const handle = await page.waitForFunction(
    (xpath: string, prop: string) => {
      const element: Node = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
        .singleNodeValue;
      return element && !element[prop];
    },
    {},
    xpathSelector,
    propertyName
  );
  return (await handle.jsonValue()) as boolean;
}

// ************************************************************************
/**
 * Helper functions when directly dealing with CSS selectors.
 */

/**
 * Wait for the element found from the selector has a particular attribute value pair
 * @param {string} cssSelector - The selector for the element on the page
 * @param {string} attribute - The attribute name
 * @param {string} value - The attribute value to match
 * @param {number} timeout - the timeout in msecs
 */
export async function waitForAttributeEquality(
  page: Page,
  selector: { xpath?: string; css?: string },
  attribute: string,
  value: string,
  timeout?: number
): Promise<boolean> {
  if (selector.css !== undefined) {
    const handle = await page.waitForFunction(
      (css: string, attributeName: string, attributeValue: string) => {
        const element: HTMLElement = document.querySelector(css) as HTMLElement;
        return (
          element && element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue
        );
      },
      { timeout: timeout || 30000 },
      selector.css,
      attribute,
      value
    );
    return (await handle.jsonValue()) as boolean;
  }
  if (selector.xpath !== undefined) {
    const handle = await page.waitForFunction(
      (xpath: string, attributeName: string, attributeValue: string) => {
        const element: HTMLElement = document.evaluate(
          xpath,
          document.body,
          null,
          XPathResult.FIRST_ORDERED_NODE_TYPE,
          null
        ).singleNodeValue as HTMLElement;
        return (
          element && element.attributes[attributeName] && element.attributes[attributeName].value === attributeValue
        );
      },
      { timeout: timeout || 30000 },
      selector.xpath,
      attribute,
      value
    );
    return (await handle.jsonValue()) as boolean;
  }
  throw new Error('Required selector: {xpath or css} is missing');
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
  selector: { xpath?: string; css?: string } = { css: 'body' },
  timeout?: number
): Promise<boolean> {
  if (selector.css !== undefined) {
    // wait for visible then compare texts
    await page.waitForSelector(selector.css, { visible: true, timeout });
    const handle = await page.waitForFunction(
      (css, expText) => {
        const element: Node = document.querySelector(css) as Node;
        const regExp = new RegExp(expText);
        return element && regExp.test(element.textContent);
      },
      { timeout },
      selector.css,
      textSubstr
    );
    return (await handle.jsonValue()) as boolean;
  }
  if (selector.xpath !== undefined) {
    await page.waitForXPath(selector.xpath, { visible: true, timeout });
    const handle = await page.waitForFunction(
      (xpath, expText) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        const regExp = new RegExp(expText);
        return element != null && regExp.test(element.textContent);
      },
      { timeout },
      selector.xpath,
      textSubstr
    );
    return (await handle.jsonValue()) as boolean;
  }
  throw new Error('xpath or css is required');
}

/**
 * <pre>
 * Wait while the page is loading (spinner is spinning and visible). Waiting stops when spinner stops spinning or when timed out.
 * It usually indicates the page is ready for user interaction.
 * </pre>
 */
export async function waitWhileLoading(
  page: Page,
  timeout: number = 2 * 60 * 1000,
  opts: { waitForRuntime?: boolean } = {}
): Promise<void> {
  const { waitForRuntime = false } = opts;

  const notBlankPageSelector = '[data-test-id="sign-in-container"], title:not(empty), div.spinner, svg[viewBox]';
  const spinElementsSelector = `[style*="running spin"], .spinner:empty, [style*="running rotation"]${
    waitForRuntime ? ':not([aria-hidden="true"])' : ''
  }`;

  // To prevent checking on blank page, wait for elements exist in DOM.
  await Promise.race([page.waitForSelector(notBlankPageSelector), page.waitForSelector(spinElementsSelector)]);

  // Wait for spinners stop and gone.
  await page.waitForFunction(
    (css) => {
      const elements = document.querySelectorAll(css);
      return elements && elements.length === 0;
    },
    { polling: 'mutation', timeout },
    spinElementsSelector
  );

  await page.waitForTimeout(500);
}
