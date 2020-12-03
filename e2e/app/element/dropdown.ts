import BaseElement from "app/element/base-element";
import {ElementHandle, Page} from "puppeteer";

export default class Dropdown extends BaseElement {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async selectOption(optionText: string): Promise<void> {
    await this.page.waitForXPath(this.getXpath()).then(d => d.click({delay: 20}));
    const option = await this.waitForOption(optionText);
    await option.click({delay: 20});
  }

  async waitForOption(optionText: string): Promise<ElementHandle> {
    const selector = this.getOptionXpath(optionText);
    return this.page.waitForXPath(selector, {visible: true});
  }

  private getOptionXpath(optionText?: string): string {
    if (optionText) {
      return `${this.getXpath()}//li[contains(text(), "${optionText}")]`;
    }
    return `${this.getXpath()}//li[contains(text(), "")]`;
  }
}
