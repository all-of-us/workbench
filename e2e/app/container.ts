import {ElementHandle, Page} from 'puppeteer';

/**
 * This is the super base class.
 * Every element needs a Page object and a xpath for locating the element.
 */
export default class Container {

  constructor(protected readonly page: Page, protected xpath?: string) { }

  getXpath(): string {
    return (this.xpath === undefined) ? '' : this.xpath;
  }

  async waitUntilVisible(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, {visible: true});
  }
}
