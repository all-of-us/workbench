import BaseElement from './base-element';
import { ElementHandle, Page } from 'puppeteer';

export default class PrimereactInputNumber extends BaseElement {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async setValue(value: number): Promise<void> {
    const input = await this.getInputElementHandle();
    // primereact InputNumber thoughtfully ignores any input that would cause the input to go outside
    // its specified mins and maxes and is very finicky around 1) copy/paste and 2) manual manipulation
    // of its value prop so instead we must do shenanigans!
    await input.click();
    while ((await this.getInputValue()) !== value) {
      const curr = await this.getInputValue();
      if (curr > value) {
        await this.page.keyboard.press('ArrowDown');
      } else if (curr < value) {
        await this.page.keyboard.press('ArrowUp');
      }
    }
  }

  async getInputValue(): Promise<number> {
    const input = await this.getInputBaseElement();
    return parseInt(await input.getProperty('value'), 10);
  }

  private async getInputBaseElement(): Promise<BaseElement> {
    return BaseElement.asBaseElement(this.page, await this.getInputElementHandle());
  }

  private async getInputElementHandle(): Promise<ElementHandle> {
    return await this.page.waitForXPath(this.getInputXPath(), { visible: true });
  }

  private getInputXPath(): string {
    return `${this.getXpath()}/input`;
  }
}
