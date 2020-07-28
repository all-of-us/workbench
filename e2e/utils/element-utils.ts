import {ElementHandle, JSHandle, Page} from 'puppeteer';

/**
 * Gets the attribute value.
 * @param {Page} page Instance of Puppeteer page object.
 * @param {ElementHandle} element Element.
 * @param {string} attribute Attribute name.
 */
export const getAttrValue = async (page: Page, element: ElementHandle, attribute: string): Promise<string> => {
  return page.evaluate((link, attr) => link.getAttribute(attr), element, attribute);
}

/**
 * Gets the property value.
 * @param {ElementHandle} element Element.
 * @param {string} property Property name.
 */
export const getPropValue = async (element: ElementHandle, property: string): Promise<string> => {
  const value = await element.getProperty(property).then((prop) => prop.jsonValue());
  return value.toString().trim();
}

/*
 * Wait until the existing element changed in DOM.
 * @param {Page} page Instance of Puppeteer page object.
 * @param {ElementHandle} element Element.
 */
export async function waitUntilChanged(page: Page, element: ElementHandle): Promise<JSHandle> {
  return page.waitForFunction(elemt => !elemt.ownerDocument.contains(elemt), {polling: 'raf'}, element);
}

export const click = async (page: Page, selector: {css?: string, xpath?: string}): Promise<void> => {
  const { css, xpath } = selector;
  if (css === undefined && xpath === undefined) {
    throw new Error('click() function requires a css or xpath.');
  }
  if (css !== undefined) {
    await page.waitForSelector(css, {visible: true});
    await page.evaluate((cssSelector) => {
      document.querySelector(cssSelector).click();
    }, css);
    return;
  }
  await page.waitForXPath(xpath, {visible: true});
  await page.evaluate((xpathSelector) => {
    const node: any = document.evaluate(xpathSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    node.click();
  }, xpath);
}

/**
 * Check whether a web element exist. Visibility ignored.
 * @param page Puppeteer.Page
 * @param selector CSS selector
 */
export const exists = async (page: Page, selector: {css?: string, xpath?: string}): Promise<boolean> => {
  const { css, xpath } = selector;
  if (css !== undefined) {
    return (await page.$$(`${selector}`)).length > 0;
  }
  return (await page.$x(xpath)).length > 0;
}
