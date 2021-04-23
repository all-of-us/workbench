import { ElementHandle, JSHandle, Page } from 'puppeteer';
import { withErrorLogging } from './error-handling';

/**
 * Gets the attribute value.
 * @param {Page} page Instance of Puppeteer page object.
 * @param {ElementHandle} element Element.
 * @param {string} attribute Attribute name.
 */
export const getAttrValue = async (page: Page, element: ElementHandle, attribute: string): Promise<string> => {
  return page.evaluate(
    (elemt, attr) => {
      return elemt.getAttribute(attr);
    },
    element,
    attribute
  );
};

/**
 * Gets the property value.
 * @param {ElementHandle} element Element.
 * @param {string} property Property name.
 */
export async function getPropValue<T>(element: ElementHandle, property: string): Promise<T> {
  const value = await element.getProperty(property).then((prop) => prop.jsonValue());
  return value as T;
}

/*
 * Wait until the existing element changed in DOM.
 * @param {Page} page Instance of Puppeteer page object.
 * @param {ElementHandle} element Element.
 */
export const waitUntilChanged = withErrorLogging({
  message: 'waitUntilChanged() failed',
  fn: async (page: Page, element: ElementHandle): Promise<JSHandle> => {
    return page.waitForFunction((elemt) => !elemt.ownerDocument.contains(elemt), { polling: 'raf' }, element);
  }
});

export async function matchText(page: Page, cssSelector: string, subString: string): Promise<boolean> {
  const texts = await page.$$eval(cssSelector, (elements) => elements.map((el) => el.textContent));
  let found = false;
  for (const txt of texts) {
    if (txt.includes(subString)) {
      found = true;
      break;
    }
  }
  return found;
}
