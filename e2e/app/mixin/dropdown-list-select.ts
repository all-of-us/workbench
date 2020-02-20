import {ElementHandle, Page} from 'puppeteer';

export default class DropdownSelect {

  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  public async select(textValue: string) {
    await this.toOpen();
    const selector = this.componentXpath() + `//li[@class='p-dropdown-item'][normalize-space(.)='${textValue}']`;
    const selectValue = await this.page.waitForXPath(selector);
    await selectValue.click();
  }

  public async displayedValue() {
    const selector = this.componentXpath() + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    return await (await displayedValue.getProperty('innerText')).jsonValue();
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
      console.log('isOpen true');
      return await this.toggleOpenClose();
    }
    console.log('isOpen false');
  }

  private async getComponent(): Promise<ElementHandle> {
    return await this.page.waitForXPath(this.componentXpath())
  }

  private componentXpath(): string {
    return '//*[@class=\'p-dropdown p-component\']';
  }

}
