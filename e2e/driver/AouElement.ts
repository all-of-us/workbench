import {ClickOptions, ElementHandle, Page} from 'puppeteer';

export default class AouElement {
  public eHandle: ElementHandle;

  constructor(handle: ElementHandle) {
    this.eHandle = handle;
  }

  public async getProperty(property: string): Promise<unknown> {
    return await (await this.eHandle.getProperty(property)).jsonValue();
  }

   /**
    * Cannot get attribute directly from a ElementHandle.
    * If the attribute have property counterparts, use getProperty function.
    */
  public async getAttribute(page: Page) {
    return await page.evaluate(link => link.getAttribute('href'), this.eHandle,);
  }

  /**
   * Get the element attribute value.
   * @param {Page} page
   * @param {ElementHandle} element
   * @param {string} attribute
   */
  public async getAttributeAlternative(page: Page, element: ElementHandle, attribute: string) {
    const handle = await page.evaluateHandle( (elem, attr) => {
      return elem.getAttribute(attr);
    }, element, attribute);
    return await handle.jsonValue();
  }

  public async click(options?: ClickOptions) {
    return (await this.eHandle.click(options));
  }

  public async isVisible() {
    return (await this.eHandle.asElement().boxModel() !== null);
  }

  public async exists() {
    return (await this.eHandle.asElement() !== null);
  }

  public async isCheckbox() {
    return (await this.getProperty('type') === 'checkbox')
  }

  public async check() {
    if (!!this.isChecked()) {
      this.click();
    }
  }

  public async unCheck() {
    if (this.isChecked()) {
      this.click();
    }
  }

  public async isDisabled() {
    return await this.getProperty('disabled');
  }

  public async isChecked() {
    return await this.getProperty('checked');
  }

  public async select(aValue: string) {
    return await this.eHandle.select(aValue);
  }

}
