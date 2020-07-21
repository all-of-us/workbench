import {ElementHandle, Page} from 'puppeteer';

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
