import { ElementHandle, Page } from 'puppeteer';

export default class AouElement {

  public eHandle: ElementHandle;

  constructor(handle: ElementHandle) {
    this.eHandle = handle;
  }

  public async getProp(property: string): Promise<unknown> {
    const prop = await this.eHandle.getProperty(property);
    return await prop.jsonValue();
  }

  /**
   * Get attribute directly from a ElementHandle.
   * If the attribute have property counterparts, use getProperty function.
   */
  public async getAttr(attr: string): Promise<unknown> {
    const atr = await this.eHandle.getAttribute(attr);
    return atr;
  }

  public async click() {
    await this.eHandle.asElement().click();
  }

  public async focus() {
    return Promise.all([
      this.eHandle.focus(),
      this.eHandle.hover()
    ]);
  }

  public async isVisible(): Promise<boolean> {
    return (await this.eHandle.asElement().boxModel() !== null);
  }

  public async exists(): Promise<boolean> {
    return (await this.eHandle.asElement() !== null);
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
    return await this.eHandle.select(aOptionalValue);
  }

  // @ts-ignore
  public async selectByText(textValue: string) {
    // todo
  }

  public async hasAttribute(attr: string): Promise<boolean> {
    if (await this.getAttr(attr)) { return; }
  }

  public async type(txt: string) {
    await this.eHandle.focus();
    await this.eHandle.type(txt);
  }

  public async press(key: string, options?: { text?: string, delay?: number }) {
    await this.eHandle.press(key, options)
  }

  public asElementHandle(): ElementHandle {
    return this.eHandle.asElement();
  }

}
