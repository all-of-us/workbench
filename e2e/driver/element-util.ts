import {ElementHandle, Page} from 'puppeteer';

/**
 * Summary: Helper functions when directly dealing with Puppeteer ElementHandle.
 */

/**
 * <pre>
 * Get the value of a particular property for a particular element
 * </pre>
 * @param {Page} page
 * @param {ElementHandle} element - The web element to get the property value for
 * @param {string} property - The property to look for
 * @returns {string} value - The property value
 */
export async function getProperty(page: Page, element: ElementHandle, property: string) {
  // Alternative: return element.getProperty(property).then((elem) => elem.jsonValue());
  const handle = await page.evaluateHandle((elem, prop) => {
    return elem[prop];
  }, element, property);
  return await handle.jsonValue();
}

/**
 * Get the element attribute value.
 * @param {Page} page
 * @param {ElementHandle} element
 * @param {string} attribute
 */
export async function getAttribute(page: Page, element: ElementHandle, attribute: string) {
  const handle = await page.evaluateHandle((elem, attr) => {
    return elem.getAttribute(attr);
  }, element, attribute);
  return await handle.jsonValue();
}

/**
 * <pre>
 * Get the textContent property value for a element.
 * </pre>
 * @param {Page} page
 * @param {ElementHandle} element - The web element
 * @returns {string} value - The text property value
 */
export async function getTextContent(page: Page, element: ElementHandle) {
  return await page.evaluate(elem => elem.textContent, element);
}

/**
 * <pre>
 * Get the value property value for a element.
 * </pre>
 * @param page
 * @param element
 */
export async function getValue(page: Page, element: ElementHandle) {
  // Alternative: await (await elem.getProperty('value')).jsonValue();
  return await page.evaluate(elem => elem.value, element);
}

/**
 * <pre>
 * Check whether a web element exist. Visibility is not checked.
 * </pre>
 * @param {Page} page
 * @param {ElementHandle} element: The web element to check
 */
export async function exists(page: Page, element: ElementHandle) {
  return await page.evaluate(elem => {
    return elem !== null;
  }, element);
}

export async function findText(page: Page, txt: string) {
  return (await page.content()).match(txt);
}
