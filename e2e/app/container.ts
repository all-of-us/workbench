import {Page} from 'puppeteer';

/**
 * This is the super base class.
 * Every element needs a Page object and a xpath for locating the element.
 */
export default class Container {

  constructor(protected readonly page: Page, protected xpath?: string) { }

  getXpath(): string {
    return (this.xpath === undefined) ? '' : this.xpath;
  }

  setXpath(xpath: string) {
    this.xpath = xpath;
  }

  async isVisible(): Promise<boolean> {
    return this.page.waitForXPath(this.xpath, {visible: true, timeout: 1000})
      .then(() => {return true})
      .catch(() => {return false});
  }

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), {visible: true});
  }

  async waitUntilClose(): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), {hidden: true});
  }

}
