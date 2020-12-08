import BaseElement from "app/element/base-element";
import {ElementHandle, Page} from "puppeteer";

export default class Dropdown extends BaseElement {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async selectOption(optionText: string): Promise<void> {
    const dropdown = await this.page.waitForXPath(this.getXpath())
    await dropdown.click({delay: 20});
    const option = await this.waitForOption(optionText);
    return await option.click({delay: 20});
  }

  async waitForOption(optionText: string): Promise<ElementHandle> {
    const selector = this.getOptionXpath(optionText);
    return await this.page.waitForXPath(selector, {visible: true, timeout: 150000});
  }

  private getOptionXpath(optionText?: string): string {
    if (optionText) {
      return `${this.getXpath()}//li[contains(text(), "${optionText}")]`;
    }
    return `${this.getXpath()}//li[contains(text(), "")]`;
  }
}
