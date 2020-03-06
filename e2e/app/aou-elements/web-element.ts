import {ClickOptions, ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import {ElementInterface} from './element-interface';

export default class WebElement implements ElementInterface {

  static asWebElement(page: Page, elem: ElementHandle): WebElement {
    return new WebElement(page, elem);
  }

  protected readonly page: Page;
  protected css: string;
  protected xpath: string;
  protected element: ElementHandle;

  constructor(aPage: Page, aElement?: ElementHandle) {
    this.page = aPage;
    this.element = aElement || undefined;
  }

  async withCss(aCssSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.css = aCssSelector;
    this.element = await this.waitForCss(options);
    return this.element;
  }

  async withXpath(aXpathSelector: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
    this.xpath = aXpathSelector;
    this.element = await this.waitForXpath(options);
    return this.element;
  }

  async waitForXpath(options?: WaitForSelectorOptions): Promise<ElementHandle> {
    if (this.xpath == null) { throw new Error('Xpath selector is undefined.'); }
    this.element = await this.page.waitForXPath(this.xpath, options);
    return this.element
  }

  async waitForCss(options?: WaitForSelectorOptions): Promise<ElementHandle> {
    if (this.css == null) { throw new Error('CSS selector is undefined.'); }
    this.element = await this.page.waitForSelector(this.css, options);
    return this.element;
  }

   /**
    * Find element without waiting for.
    */
  async findByCss(): Promise<ElementHandle | null> {
    if (this.css == null) { throw new Error('CSS selector is undefined.'); }
    this.element = await this.page.$(this.css);
    return this.element;
  }

   /**
    * Find element without waiting for.
    */
  async findByXpath(): Promise<ElementHandle> {
    if (this.xpath == null) { throw new Error('Xpath selector is undefined.'); }
    this.element = (await this.page.$x(this.xpath))[0];
    return this.element;
  }

  /**
   * Finds the value of a property
   */
  async getProperty(propertyName: string): Promise<unknown> {
    if (this.element == null) { throw new Error('The element is undefined.'); }
    const p = await this.element.asElement().getProperty(propertyName);
    return await p.jsonValue();
  }

  /**
   * Finds the value of an attribute
   */
  async getAttribute(attributeName: string): Promise<string | null> {
    if (this.element == null) { throw new Error('The element is undefined.'); }
    const elem = this.element.asElement();
    const attributeValue = await this.page.evaluate(
       (link, attr) => link.getAttribute(attr), elem, attributeName
    );
    return attributeValue;
  }

  /**
   * Call one of following functions before call this function.
   * findByCss
   *
   * @param attributeName
   */
  async hasAttribute(attributeName: string): Promise<boolean> {
    if (this.element == null) { throw new Error('The element is undefined.'); }
    const value = await this.getAttribute(attributeName);
    return value !== null;
  }

  /**
   * Disabled means element has `disabled` attribute.
   */
  async isDisabled(): Promise<boolean> {
    const disabled = await this.getProperty('disabled');
    return !!disabled ;
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
    await this.element.asElement().click(options);
  }

  async type(text: string, options?: { delay: number }): Promise<void> {
    await this.focus();
    await this.element.asElement().type(text, options);
  }

  async pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void> {
    await this.element.asElement().press(key, options);
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

  async getTextContent(): Promise<string> {
    const handle = await this.element.asElement();
    return await handle.evaluate(
       (element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''), this.element,
    );
  }

  async getValue(): Promise<unknown> {
    return await this.getProperty('value');
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
   * Finds element's size.
   */
  async size(): Promise<{ width: number; height: number }> {
    const box = await this.element.boundingBox();
    if (!box) { return { width: 0, height: 0 }; }
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
    await this.page.evaluate(elem =>
       elem.click(), this.element );
  }

}
