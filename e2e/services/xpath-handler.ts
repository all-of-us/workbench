import {Page} from 'puppeteer-core';

/**
 * Click on element found by specified xpath selector.
 * @param {string} xpathSelector Xpath selector
 */
export async function click(xpathSelector: string) {
  const elem = await this.puppeteerPage.waitForXPath(xpathSelector, {visible: true});
  return await elem.click();
}

export async function type(xpathSelector: string, texts: string) {
  const elem = await this.puppeteerPage.waitForXPath(xpathSelector, {visible: true});
  return await elem.type(texts);
}

export async function waitForProperty(page: Page, xpathSelector: string, propertyName: string, propertyValue: string) {
  return await page.waitForFunction( xpath => {
    const element = document.evaluate(
       xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    return element[propertyName] === propertyValue;
  }, {}, xpathSelector);
}

export async function waitForText(page: Page, xpathSelector: string, expectedText: string) {
  return await page.waitForFunction( xpath => {
    const element = document.evaluate(
       xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    return element.textContent === expectedText
  }, {}, xpathSelector);
}

export async function exists(page: Page, xpathSelector: string) {
  return await page.waitForFunction( xpath => {
    return !document.evaluate(
       xpath, document.body, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
  }, {}, xpathSelector);
}

export async function waitForExists(page: Page, xpathSelector: string) {
  await page.waitForXPath(xpathSelector);
}

export async function waitForVisible(page: Page, xpathSelector: string) {
  return await page.waitForXPath(xpathSelector, {visible: true});
}

export async function waitForHidden(page: Page, xpathSelector: string) {
  return await page.waitForXPath(xpathSelector, {hidden: true});
}
