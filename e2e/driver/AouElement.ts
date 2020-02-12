import {ClickOptions, ElementHandle, Page} from 'puppeteer';

export default class AouElement {
  public eHandle: ElementHandle;

  constructor(handle: ElementHandle) {
    this.eHandle = handle;
  }

  public async getProperty(property: string): Promise<unknown> {
    const prop = await this.eHandle.getProperty(property);
    console.log("getProperty function: " + await prop.jsonValue());
    return await prop.jsonValue();
  }

   /**
    * Get attribute directly from a ElementHandle.
    * If the attribute have property counterparts, use getProperty function.
    */
  public async getAttr(attr: string) {
    return await this.eHandle.getAttribute(attr);
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

  public async click() {
    // await this.eHandle.asElement().click(options);
    await this.eHandle.clicking;
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
    const pValue = await this.isChecked();
    if (!pValue) {
      console.log('going to check the checkbox');
      await this.click();
    }
  }

  public async unCheck() {
    const pValue = await this.isChecked();
    if (pValue) {
      await this.click();
    }
  }

  public async isDisabled() {
    return await this.getProperty('disabled');
  }

  public async isChecked() {
    const checkedProp = await this.getProperty('checked');
    console.log('isChecked function: ' + checkedProp);
    return checkedProp;
  }

  public async select(aValue: string) {
    return await this.eHandle.select(aValue);
  }

  public async hasAttribute(attr: string) {
    if (await this.getAttr(attr)) { return; }
  }

  public async type(txt: string) {
    await this.eHandle.focus();
    await this.eHandle.type(txt);
  }

  public asElement() {
    return this.eHandle.asElement();
  }

}
