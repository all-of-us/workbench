import {ClickOptions, ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';

/**
 * BaseElement represents a web element in the DOM.
 * It implements useful methods for querying and interacting with this element.
 */
export default class BaseElement {

  static asBaseElement(page: Page, elem: ElementHandle): BaseElement {
    return new BaseElement(page, elem);
  }

  protected readonly page: Page;
  protected css: string;
  protected xpath: string;
  protected element: ElementHandle;

  constructor(aPage: Page, aElement?: ElementHandle) {
    this.page = aPage;
    this.element = aElement || undefined;
  }

  /**
   * Find element with wait.
   * @param cssSelector
   * @param waitOptions
   */
  async withCss(cssSelector: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.css = cssSelector;
    this.element = await this.page.waitForSelector(this.css, waitOptions);
    return this.element;
  }

  /**
   * Find element with wait.
   * @param xpathSelector
   * @param waitOptions
   */
  async withXpath(xpathSelector: string, waitOptions?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.xpath = xpathSelector;
    this.element = await this.page.waitForXPath(this.xpath, waitOptions);
    return this.element;
  }

   /**
    * Find first element without wait for.
    */
  async findByCss(cssSelector: string,): Promise<ElementHandle | null> {
    this.css = cssSelector;
    this.element = await this.page.$(this.css);
    return this.element;
  }

   /**
    * Find first element without wait for.
    */
  async findByXpath(xpathSelector: string): Promise<ElementHandle | null> {
    this.xpath = xpathSelector;
    const found = await this.page.$x(this.xpath);
    if (found.length > 0) {
      this.element = found[0];
    } else {
      this.element = null;
    }
    return this.element;
  }

  async retryFindElement(): Promise<ElementHandle | null> {
    if (this.xpath != null) {
      return this.findByXpath(this.xpath);
    } else if (this.css != null) {
      return this.findByCss(this.css);
    }
    return null;
  }

  /**
   * Finds the value of a property for this element.
   *
   * Alternative:
   *  const handle = await page.evaluateHandle((elem, prop) => {
   *    return elem[prop];
   *  }, element, property);
   *  return await handle.jsonValue();
   */
  async getProperty(propertyName: string): Promise<unknown> {
    if (this.element == null) {
      throw new Error('The element is undefined.');
    }
    const p = await this.element.asElement().getProperty(propertyName);
    return await p.jsonValue();
  }

  /**
   * Finds the value of an attribute
   * @param attribute name
   */
  async getAttribute(attributeName: string): Promise<string | null> {
    if (this.element == null) {
      throw new Error('The element is undefined.');
    }
    const elem = this.element.asElement();
    const attributeValue = await this.page.evaluate(
       (link, attr) => link.getAttribute(attr), elem, attributeName);
    return attributeValue;
  }

  /**
   * Does attribute exists for this element?
   *
   * @param attribute name
   */
  async hasAttribute(attributeName: string): Promise<boolean> {
    if (this.element == null) {
      throw new Error('The element is undefined.');
    }
    const value = await this.getAttribute(attributeName);
    return value !== null;
  }

  /**
   * Is element disabled or readonly?
   * Disabled means element has `disabled` attribute.
   */
  async isDisabled(): Promise<boolean> {
    const disabled = await this.getProperty('disabled');
    return !!disabled;
  }

  /**
   * <pre>
   *  Check if the element is visible
   * </pre>
   * @param {Page} page
   * @param {ElementHandle} element
   */
  async isVisible(): Promise<boolean> {
    const boxModel = await this.element.boxModel();
    return boxModel !== null;
  }

  /**
   * Check both boxModel and style for visibility.
   */
  async isDisplayed() {
    const isVisibleHandle = await this.page.evaluateHandle((e) =>
    {
      const style = window.getComputedStyle(e);
      return (style && style.display !== 'none' &&
         style.visibility !== 'hidden' && style.opacity !== '0');
    }, this.element);
    const jValue = await isVisibleHandle.jsonValue();
    const boxModelValue = await this.element.boxModel();
    if (jValue && boxModelValue) {
      return true;
    }
    return false;
  }

  async click(options?: ClickOptions): Promise<void> {
    return this.element.asElement().click(options);
  }

  async type(text: string, options?: { delay: number }): Promise<void> {
    await this.focus();
    return this.element.asElement().type(text, options);
  }

  async pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void> {
    return this.element.asElement().press(key, options);
  }

  /**
   * Press keyboard "tab".
   */
  async tabKey(): Promise<void> {
    return this.pressKeyboard('Tab', { delay: 100 });
  }

  /**
   * Clear value in textbox or textarea.
   */
  async clear(): Promise<void> {
    await this.element.click({clickCount: 3});
    await this.element.press('Backspace');
  }

  /**
   * Calling focus() and hover() together.
   */
  async focus(): Promise<void> {
    const handle = this.element.asElement();
    await Promise.all([
      handle.focus(),
      handle.hover()
    ]);
  }

  /**
   * <pre>
   * Get the textContent property value for a element.
   * </pre>
   */
  async getTextContent(): Promise<string> {
    const handle = await this.element.asElement();
    return handle.evaluate(
       (element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''), this.element,
    );
  }

  /**
   * Get the value of property 'value' for this element.
   * Alternative: await page.evaluate(elem => elem.value, element);
   */
  async getValue(): Promise<unknown> {
    return this.getProperty('value');
  }

  async getComputedStyle(styleName: string): Promise<unknown> {
    const handle = this.element.asElement();
    const attrStyle = await handle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style;
    }, this.element);
    return (await attrStyle.getProperty(styleName)).jsonValue()
  }

  /**
   * Determine if cursor is disabled (== " not-allowed ") by checking style 'cursor' value.
   */
  async isCursorNotAllowed(): Promise<boolean> {
    const cursor = await this.getComputedStyle('cursor');
    return cursor === 'not-allowed';
  }

  /**
   * Finds element's size.
   */
  async getSize(): Promise<{ width: number; height: number }> {
    const box = await this.element.boundingBox();
    if (!box) {
      // if element is not visible, returns size of (0, 0).
      return { width: 0, height: 0 };
    }
    const { width, height } = box;
    return { width, height };
  }

  async dispose(): Promise<void> {
    this.xpath = undefined;
    this.css = undefined;
    return this.element.dispose();
  }

  // try this method when click() is not working
  async clickWithEval() {
    return this.page.evaluate( elem => elem.click(), this.element );
  }

}
