import {Page} from 'puppeteer';

export async function clickByXpath(page: Page, xpathSelector: string) {
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

export async function clickByCss(page: Page, cssSelector: string) {
  return page.evaluate((selector) => {
    const node: any = document.querySelector(selector);
    node.click();
  }, cssSelector);

}

/**
 * Summary: Helper functions when directly dealing with CSS selectors.
 */

/**
 * Is there a element located by CSS selector?
 *
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function exists(page: Page, selector: string) {
  return !!(await page.$(`${selector}`));
}

/**
 * Is there a element and visible located by CSS selector?
 *
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export async function visible(page: Page, selector: string) {
  return page.evaluate(css => {
    const element = document.querySelector(css);
    return !!(element && (element.offsetWidth || element.offsetHeight || element.getClientRects().length))
  }, selector)
}
