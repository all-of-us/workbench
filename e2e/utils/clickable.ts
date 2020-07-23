import {Page} from 'puppeteer';

export async function click(page: Page, selector: {css?: string, xpath?: string}): Promise<void> {
  const { css, xpath } = selector;
  if (css === undefined && xpath === undefined) {
    throw new Error('click() function requires a css or xpath.');
  }
  if (css !== undefined) {
    await page.waitForSelector(css, {visible: true});
    await page.evaluate((cssSelector) => {
      const node: any = document.querySelector(cssSelector);
      node.click();
    }, css);
  }
  await page.waitForXPath(xpath, {visible: true});
  await page.evaluate((xpathSelector) => {
    const node: any = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    node.click();
  }, xpath);
}
