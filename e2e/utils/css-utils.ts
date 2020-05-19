import {Page} from 'puppeteer';

/**
 * Summary: Helper functions when directly dealing with CSS selectors.
 */

export async function clickByEvalXpath(page: Page, xpathSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.evaluate(
       selector,
       document,
       null,
       XPathResult.FIRST_ORDERED_NODE_TYPE,
       null
    ).singleNodeValue;
    document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
    node.click();
  }, xpathSelector);
}

export async function clickByEvalCss(page: Page, cssSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.querySelector(selector);
    node.click();
  }, cssSelector);
}

/**
 * Is there a element located by CSS selector?
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string) {
  return !!(await page.$(`${selector}`));
}
