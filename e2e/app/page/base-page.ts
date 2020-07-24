import {ElementHandle, Page, Response} from 'puppeteer';

/**
 * All Page Object classes will extends the BasePage.
 * Contains common functions/actions that help with tests creation.
 */
export default abstract class BasePage {

  page: Page;

  protected constructor(page: Page) {
    this.page = page;
  }

  async getPageTitle() : Promise<string> {
    return this.page.title();
  }

  /**
   * Reload current page.
   */
  async reloadPage(): Promise<Response> {
    return await this.page.reload( { waitUntil: ['networkidle0', 'load'] } );
  }

  /**
   * Load a URL.
   */
  async gotoUrl(url: string): Promise<void> {
    await this.page.goto(url, {waitUntil: ['networkidle0', 'load']});
  }

  /**
   *  <pre>
   * Get the element's textContent for a specified CSS selector.
   *  </pre>
   * @param {string} cssSelector
   * @returns {string} value - The text property value
   */
  async getTextContent(cssSelector: string): Promise<string> {
    return this.page.$eval(`${cssSelector}`, elem => elem.textContent.trim())
  }

  /**
   * Find texts in DOM.
   * @param txt
   */
  async containsText(txt: string): Promise<boolean> {
    const indx = await (await this.page.content()).indexOf(txt);
    return indx !== -1;
  }

  /**
   * <pre>
   * Get the value of a particular property for a particular element
   * </pre>
   * @param {ElementHandle} element - The web element to get the property value for
   * @param {string} property - The property to look for
   * @returns {string} value - The property value
   */
  async getProperty(element: ElementHandle, property: string): Promise<string> {
    // Alternative: return element.getProperty(property).then((elem) => elem.jsonValue());
    const handle = await this.page.evaluateHandle((elem, prop) => {
      return elem[prop];
    }, element, property);
    return (await handle.jsonValue()).toString();
  }

  /**
   * Get the element attribute value.
   * @param {ElementHandle} element
   * @param {string} attribute
   */
  async getAttribute(element: ElementHandle, attribute: string): Promise<string> {
    const handle = await this.page.evaluateHandle((elem, attr) => {
      return elem.getAttribute(attr);
    }, element, attribute);
    return (await handle.jsonValue()).toString();
  }

  /**
   * <pre>
   * Check whether a web element exist. Visibility is not checked.
   * </pre>
   * @param {ElementHandle} element: The web element to check
   */
  async exists(element: ElementHandle): Promise<boolean> {
    return await this.page.evaluate(elem => {
      return elem !== null;
    }, element);
  }

}
