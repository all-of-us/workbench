import { ElementHandle, Page } from 'puppeteer';
import input from "../app/elements/input";

export default class AouElement {

  private element: ElementHandle;

  constructor(elementhandle: ElementHandle) {
    this.element = elementhandle;
  }

  public async getProp(property: string): Promise<unknown> {
    const prop = await this.element.getProperty(property);
    return await prop.jsonValue();
  }

  /**
   * Get attribute directly from a ElementHandle.
   * If the attribute have property counterparts, use getProperty function.
   */
  public async getAttr(attribute: string): Promise<unknown> {
    const attr = await this.element.evaluate( (node, attri) => node.getAttribute(attri), attribute);
    return attr;
  }

  public async click() {
    await this.element.asElement().click();
  }

  public async focus() {
    return Promise.all([
      this.element.focus(),
      this.element.hover()
    ]);
  }

  public async isVisible(): Promise<boolean> {
    return (await this.element.asElement().boxModel() !== null);
  }

  public async exists(): Promise<boolean> {
    return (await this.element.asElement() !== null);
  }

  public async isCheckbox() {
    return (await this.getProp('type') === 'checkbox')
  }

  public async isButton() {
    return (await this.getProp('type') === 'button')
  }

  public async check() {
    const cValue = await this.isChecked();
    if (!cValue) {
      await this.click();
    }
  }

  public async unCheck() {
    const cValue = await this.isChecked();
    if (cValue) {
      await this.click();
    }
  }

  public async isDisabled(): Promise<boolean> {
    return await this.getProp('disabled') === true;
  }

  public async isReadOnly(): Promise<boolean> {
    return await this.getProp('readOnly') === true;
  }

  public async value(): Promise<string> {
    const p = await this.getProp('value');
    if (typeof p === 'undefined') { return p; } else { return ''; }
  }

  public async isChecked(): Promise<boolean> {
    const checkedProp = await this.getProp('checked');
    return checkedProp === true;
  }

  public async selectByValue(aOptionalValue: string) {
    return await this.element.select(aOptionalValue);
  }

  public async hasAttribute(attr: string): Promise<boolean> {
    if (await this.getAttr(attr)) { return; }
  }

  public async type(txt: string) {
    await this.element.focus();
    await this.element.type(txt);
  }

  public async press(key: string, options?: { text?: string, delay?: number }) {
    await this.element.press(key, options)
  }

  public asElement(): ElementHandle {
    return this.element.asElement();
  }

  public asInput(page: Page): input {
    return null;
  }

}
