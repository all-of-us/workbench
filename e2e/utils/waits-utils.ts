import { ElementHandle, Page } from 'puppeteer';
import { logger } from 'libs/logger';
import { takeScreenshot } from './save-file-utils';
import Container from 'app/container';
import { makeDateTimeStr } from './str-utils';
import { getAttrValue } from './element-utils';

export const waitForFn = async (
  fn: () => any,
  interval = 2000,
  timeout = 10000
): Promise<boolean> => {
  const start = Date.now();
  while (Date.now() < start + timeout) {
    if (fn()) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
  return false;
};

export const waitForAsyncFn = async (fn: () => Promise<any>, intervalMs: number, timeoutMs: number): Promise<boolean> => {
  const start = Date.now();
  while (Date.now() < start + timeoutMs) {
    if ((await fn()) === true) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
  return false;
};

/**
 * Wait for the window location to match sub-string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} urlSubstr - The URL partial string.
 */
export async function waitForUrl(page: Page, urlSubstr: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(
      (txt) => {
        const href = window.location.href;
        return href.includes(txt);
      },
      {},
      urlSubstr
    );
    return (await jsHandle.jsonValue()) as boolean;
  } catch (err) {
    logger.error(`waitForUrl() failed: not contains "${urlSubstr}"`);
    logger.error(err);
    throw new Error(err);
  }
}

/**
 * Wait for the document title to match string.
 * @param {Page} page The Puppeteer Page.
 * @param {string} titleSubstr The Document title partial string.
 */
export async function waitForDocumentTitle(page: Page, titleSubstr: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(
      (t) => {
        const regExp = new RegExp(t);
        const actualTitle = document.title;
        return actualTitle && regExp.test(actualTitle);
      },
      { timeout: 60 * 1000 },
      titleSubstr
    );
    return (await jsHandle.jsonValue()) as boolean;
  } catch (err) {
    logger.error(
      `Failed to find the document title contains "${titleSubstr}". Actual page title is "${await page.title()}"`
    );
    logger.error(err.stack);
    throw new Error(err);
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
  propertyValue: string
): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(
      (xpath, prop, value) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        return element[prop as string] === value;
      },
      {},
      xpathSelector,
      propertyName,
      propertyValue
    );
    await jsHandle.jsonValue();
    return true;
  } catch (err) {
    logger.error(
      `waitForPropertyEquality() failed: xpath="${xpathSelector}" property:${propertyName} value:${propertyValue}`
    );
    logger.error(err);
    throw new Error(err);
  }
}

export async function waitForNumericalString(page: Page, xpath: string, timeout?: number): Promise<string> {
  await page.waitForXPath(xpath, { visible: true, timeout });
  const numbers = await page
    .waitForFunction(
      (xpathSelector) => {
        const node = document.evaluate(xpathSelector, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
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
    )
    .catch((err) => {
      logger.error(`waitForNumericalString() failed: xpath="${xpath}"`);
      logger.error(err.stack);
      throw new Error(err);
    });

  return (await numbers.jsonValue()).toString();
}

export async function waitForPropertyNotExists(
  page: Page,
  xpathSelector: string,
  propertyName: string
): Promise<boolean> {
  try {
    await page.waitForFunction(
      (xpath, prop) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        return !element[prop as string];
      },
      {},
      xpathSelector,
      propertyName
    );
    return true;
  } catch (err) {
    logger.error(`waitForPropertyNotExists() failed: xpath="${xpathSelector}" property: ${propertyName}`);
    logger.error(err);
    throw new Error(err);
  }
}

export async function waitForPropertyExists(page: Page, xpathSelector: string, propertyName: string): Promise<boolean> {
  try {
    await page.waitForFunction(
      (xpath, prop) => {
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        return element[prop as string] !== null;
      },
      {},
      xpathSelector,
      propertyName
    );
    return true;
  } catch (err) {
    logger.error(`waitForPropertyExists() failed: xpath="${xpathSelector}" property: ${propertyName}`);
    logger.error(err);
    throw new Error(err);
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
    const jsHandle = await page.waitForFunction(
      (selector) => {
        const elem = document.querySelector(selector);
        const isVisible = elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length;
        return !!isVisible;
      },
      {},
      cssSelector
    );
    await jsHandle.jsonValue();
    return true;
  } catch (err) {
    logger.error(`waitForVisible() failed: css="${cssSelector}"`);
    logger.error(err);
    throw new Error(err);
  }
}

/**
 * Wait while the selector is visible. Stops waiting when no longer has visible contents.
 * @param {Page} page The Puppeteer Page
 * @param {string} cssSelector The CSS selector
 */
export async function waitForHidden(page: Page, cssSelector: string): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(
      (selector) => {
        const elem = document.querySelector(selector);
        const isVisible = elem && (elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length);
        return !isVisible;
      },
      {},
      cssSelector
    );
    return (await jsHandle.jsonValue()) as boolean;
  } catch (err) {
    logger.error(`waitForHidden() failed: css="${cssSelector}"`);
    logger.error(err);
    throw new Error(err);
  }
}

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
    try {
      const jsHandle = await page.waitForFunction(
        (css, attributeName, attributeValue) => {
          const element = document.querySelector(css);
          if (element != null) {
            return (
              element.attributes[attributeName as string] &&
              element.attributes[attributeName as string].value === attributeValue
            );
          }
          return false;
        },
        { timeout: timeout || 30000 },
        selector.css,
        attribute,
        value
      );
      return (await jsHandle.jsonValue()) as boolean;
    } catch (err) {
      logger.error(`waitForAttributeEquality() failed: css=${selector.css} attribute=${attribute} value=${value}`);
      logger.error(err);
      throw new Error(err);
    }
  }
  if (selector.xpath !== undefined) {
    try {
      const jsHandle = await page.waitForFunction(
        (xpath, attributeName, attributeValue) => {
          const element: any = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue;
          return (
            element &&
            element.attributes[attributeName as string] &&
            element.attributes[attributeName as string].value === attributeValue
          );
        },
        { timeout: timeout || 30000 },
        selector.xpath,
        attribute,
        value
      );
      return (await jsHandle.jsonValue()) as boolean;
    } catch (err) {
      logger.error(`waitForAttributeEquality() failed: xpath=${selector.xpath} attribute=${attribute} value=${value}`);
      logger.error(err);
      throw new Error(err);
    }
  }
  throw new Error('Required selector: {xpath or css} is missing');
}

/**
 * Wait for the elements count to be a particular value
 * @param {string} cssSelector - The css selector for the element
 * @param {number} expectedCount - The expected number of elements
 */
export async function waitForNumberElements(page: Page, cssSelector: string, expectedCount: number): Promise<boolean> {
  try {
    const jsHandle = await page.waitForFunction(
      (selector, count) => {
        return document.querySelectorAll(selector).length === count;
      },
      {},
      cssSelector,
      expectedCount
    );
    return (await jsHandle.jsonValue()) as boolean;
  } catch (err) {
    logger.error(`waitForNumberElements() failed: css="${cssSelector}" count=${expectedCount}`);
    logger.error(err);
    throw new Error(err);
  }
}

/**
 * Wait for visible texts to match.
 * @param page
 * @param textStr
 * @param opts
 */
export async function waitForText(
  page: Page,
  textStr: string,
  opts: { timeout?: number; container?: Container; rootXpath?: string } = {}
): Promise<void> {
  const { timeout = 30 * 1000, container, rootXpath } = opts;
  let xpath: string;
  if (container) {
    xpath = container.getXpath();
  } else if (rootXpath) {
    xpath = rootXpath;
  } else {
    xpath = '//*[@data-test-id="signed-in" or @data-test-id="sign-in-page"]';
  }

  try {
    await page.waitForFunction(
      (xpath, text) => {
        const regExp = new RegExp(text);
        const element = document.evaluate(xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        return element && regExp.test(element.textContent);
      },
      { timeout: timeout },
      xpath,
      textStr
    );
  } catch (err) {
    logger.error(`ERROR: Failed to find text "${textStr}" (in xpath: '${xpath}').`);
    logger.error(err);
    throw new Error(err);
  }
}

/**
 * Wait while loading spinner is visible.
 * @param page
 * @param opts
 */
export async function waitWhileSpinnerDisplayed(
  page: Page,
  opts: { includeRuntimeSpinner?: boolean; timeout?: number; takeScreenshotOnFailure?: boolean } = {}
): Promise<void> {
  const { timeout = 2 * 60 * 1000, includeRuntimeSpinner = false, takeScreenshotOnFailure = true } = opts;
  const spinnerCss =
    '[style*="running spin"], .spinner:empty, ' +
    `[style*="running rotation"]${
      includeRuntimeSpinner ? '' : ':not([aria-hidden="true"]):not([data-test-id*="runtime-status"])'
    }, ` +
    `[style*="animation-name: spin"]${
      includeRuntimeSpinner ? '' : ':not([aria-hidden="true"]):not([data-test-id*="extraction-status"])'
    }`;
  const confidenceLevel = 2;
  let confidenceCounter = 0;
  let error;
  const waitForHidden = async (maxTime: number): Promise<void> => {
    const startTime = Date.now();
    try {
      await Promise.all([
        page.waitForFunction(
          (css) => {
            return !document.querySelectorAll(css).length;
          },
          { polling: 'mutation', timeout: maxTime },
          spinnerCss
        ),
        page.waitForSelector(spinnerCss, { hidden: true, visible: false, timeout: maxTime })
      ]);
      confidenceCounter++;
    } catch (err) {
      error = err;
    }

    if (confidenceCounter >= confidenceLevel) {
      return; // success
    }
    const spentTime = Date.now() - startTime;
    if (spentTime > maxTime) {
      if (takeScreenshotOnFailure) {
        logger.error(`ERROR: Loading spinner has not stopped. Spinner css is: ${spinnerCss}`);
        logger.error(error.stack);
        await takeScreenshot(page, makeDateTimeStr('ERROR_Spinner_TimeOut'));
      }
      throw new Error(error.message);
    }

    await page.waitForTimeout(500);
    await waitForHidden(maxTime - spentTime); // unused time
  };

  await waitForHidden(timeout);
}

/**
 * Wait while the page is loading.
 */
export async function waitWhileLoading(
  page: Page,
  opts: { includeRuntimeSpinner?: boolean; timeout?: number; takeScreenshotOnFailure?: boolean } = {}
): Promise<void> {
  const isValidPage = await assertValidPage(page, 2 * 60 * 1000);
  if (!isValidPage) {
    return;
  }
  await waitWhileSpinnerDisplayed(page, opts);
}

/**
 * Function verifies the page is a AoU application page and html documents has been loaded.
 * @param page: Page
 * @param timeout: number
 * @returns Returns False if page is the Login or Create Account page. Otherwise, returns True for all other application pages.
 */
async function assertValidPage(page: Page, timeout: number): Promise<boolean> {
  const foundElement: ElementHandle = await Promise.race([
    page.waitForXPath(process.env.AUTHENTICATED_TEST_ID_XPATH, { timeout }), // authenticated page
    page.waitForXPath(process.env.UNAUTHENTICATED_TEST_ID_XPATH, { timeout })
  ]);

  // Prevent checking in Login and Create Account pages.
  const dataTestIdValue = await getAttrValue(page, foundElement, 'data-test-id');
  if (dataTestIdValue === 'sign-in-page') {
    return false;
  }

  const notBlankTitleXpath =
    '//title[starts-with(text(), "[Test]")' +
    ' or starts-with(text(), "[Local->Local]")' +
    ' or starts-with(text(), "[Staging]")' +
    ' or starts-with(text(), "[Local->Test]")]';
  const notBlankPageCss = 'div:not(empty):not([id="root"]):not([id="body"])';

  // Prevent checking in non-authenticated and blank pages. Throws error if find all fails.
  await Promise.all([
    page.waitForXPath(process.env.AUTHENTICATED_TEST_ID_XPATH, { timeout }),
    page.waitForSelector(notBlankPageCss, { timeout }),
    page.waitForXPath(notBlankTitleXpath, { timeout })
  ]);
  return true;
}

export async function waitUntilEnabled(page: Page, cssSelector: string): Promise<boolean> {
  const jsHandle = await page
    .waitForFunction(
      (xpathSelector) => {
        const element = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
          .singleNodeValue;
        const style = window.getComputedStyle(element as Element);
        const propValue = style.getPropertyValue('cursor');
        return propValue === 'pointer';
      },
      {},
      cssSelector
    )
    .catch((err) => {
      logger.error(`waitUntilEnabled() failed: spinner css="${cssSelector}"`);
      logger.error(err);
      throw new Error(err);
    });
  return (await jsHandle.jsonValue()) as boolean;
}
