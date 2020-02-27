import {ClickOptions, ElementHandle} from 'puppeteer';
import {ElementInterface} from './web-element-interface';

export default class WebElement implements ElementInterface {

  protected readonly element: ElementHandle;

  constructor(aElementHandle: ElementHandle) {
    this.element = aElementHandle;
  }

  public async click(options?: ClickOptions): Promise<void> {
    return this.element.asElement().click(options);
  }

  public async type(text: string, options?: { delay: number }): Promise<void> {
    await this.focus();
    return this.element.asElement().type(text, options);
  }

  public async pressKeyboard(key: string, options?: { text?: string, delay?: number }): Promise<void> {
    return this.element.asElement().press(key, options);
  }

  /**
   * Finds the value of a property
   */
  public async getProperty(propertyName: string): Promise<unknown> {
    const p = await this.element.asElement().getProperty(propertyName);
    return p.jsonValue();
  }

  /**
   * Finds the value of an attribute
   */
  public async getAttribute(attributeName: string): Promise<string | null> {
    return await this.element.evaluate(
         (e: HTMLElement, attr: string) => e.getAttribute(attr), this.element, attributeName,
      );
  }

  public async hasAttribute(attributeName: string): Promise<boolean> {
    return await this.getAttribute(attributeName) !== null;
  }

  public async getTextContent(): Promise<string> {
    return await this.element.evaluate(
         (element: HTMLElement) => (element.textContent ? element.textContent.trim() : ''), this.element,
      );
  }

  /**
   * Disabled means element have a `disabled` attribute
   */
  public async isDisabled(): Promise<boolean> {
    const disabled = await this.getProperty('disabled');
    return !!disabled ;
  }

  public async isVisible(): Promise<boolean> {
    const eHandle = this.element.asElement();
    if (eHandle === null) return false;
    return await eHandle.boundingBox() !== null;
  }

  /**
   * Calling focus() and hover() together.
   */
  public async focus(): Promise<void> {
    const eHandle = this.element.asElement();
    await Promise.all([
      eHandle.focus(),
      eHandle.hover()
    ]);
  }

  public async getComputedStyle(styleName: string): Promise<unknown> {
    const eHandle = this.element.asElement();
    const attrStyle = await eHandle.evaluateHandle((e) => {
      const style = window.getComputedStyle(e);
      return style;
    }, this.element);

    return (await attrStyle.getProperty(styleName)).jsonValue()
  }

  /**
   * non-async. Is element exists in DOM. Don't check on visibility.
   */
  public exists(): boolean {
    return this.element.asElement() !== null;
  }

  public getElementHandle() {
    return this.element.asElement();
  }

}
