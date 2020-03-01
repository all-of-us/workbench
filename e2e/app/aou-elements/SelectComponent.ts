import {Page} from 'puppeteer';

export default class SelectComponent {

  private readonly page: Page;
  private readonly name: string;

  constructor(page: Page, name?: string) {
    this.page = page;
    this.name = name || undefined;
  }

  public async select(textValue: string) {
    await this.toOpen();
    const selector = this.componentXpath() + `//li[@class='p-dropdown-item'][normalize-space(.)="${textValue}"]`;
    const selectValue = await this.page.waitForXPath(selector, { visible: true });
    await selectValue.click();
  }

  public async getSelectedValue() {
    const selector = this.componentXpath() + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    const jValue = await (await displayedValue.getProperty('innerText')).jsonValue();
    return jValue;
  }

  private async toggleOpenClose() {
    const selector = this.componentXpath() + '/*[contains(@class,"p-dropdown-trigger")]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.click();
  }

  private async isOpen() {
    const selector = this.componentXpath() + '/*[contains(@class,"p-dropdown-panel")]';
    const panel = await this.page.waitForXPath(selector);
    const classNameString = await (await panel.getProperty('className')).jsonValue();
    const splitNames = classNameString.toString().split(' ');
    return splitNames.includes('p-input-overlay-visible');
  }

  private async toOpen() {
    if (!!this.isOpen()) {
      return await this.toggleOpenClose();
    }
  }

  private componentXpath(): string {
    if (this.name === undefined) {
      return '//*[contains(@class,"p-dropdown p-component")]';
    }
    return `//*[child::*[normalize-space()='${this.name}']]/*[contains(@class,'p-dropdown p-component')]`;
  }

}
