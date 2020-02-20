import {Page} from 'puppeteer';

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
export async function visible (page: Page, selector: string) {
  return page.evaluate(css => {
    const element = document.querySelector(css);
    return !!(element && (element.offsetWidth || element.offsetHeight || element.getClientRects().length))
  }, selector)
}
