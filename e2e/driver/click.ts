import {Page} from 'puppeteer';

export async function clickByXpath(page: Page, xpathSelector: string) {
  await page.evaluate((selector) => {
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
  await page.evaluate((selector) => {
    const node: any = document.querySelector(selector);
    node.click();
  }, cssSelector);

}
