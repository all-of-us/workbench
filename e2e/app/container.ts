import { ElementHandle, Page } from 'puppeteer';
import { waitWhileLoading } from 'utils/waits-utils';
import { withErrorLogging } from 'utils/error-handling';

/**
 * This is the super base class.
 * Every element needs a Page object and a xpath for locating the element.
 */
export default class Container {
  constructor(protected readonly page: Page, protected xpath?: string) {}

  getXpath(): string {
    return this.xpath === undefined ? '' : this.xpath;
  }

  setXpath(xpath: string): void {
    this.xpath = xpath;
  }

  async isVisible(timeout = 1000): Promise<boolean> {
    return this.page
      .waitForXPath(this.xpath, { visible: true, timeout })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }

  async waitUntilVisible(): Promise<void> {
    withErrorLogging({
      fn: async (): Promise<void> => {
        await Promise.all([
          waitWhileLoading(this.page, 60000),
          this.page.waitForXPath(this.getXpath(), { visible: true, timeout: 60000 })
        ]);
      }
    });
  }

  async waitUntilClose(): Promise<void> {
    withErrorLogging({
      fn: async (): Promise<void> => {
        await Promise.all([
          waitWhileLoading(this.page, 60000),
          this.page.waitForXPath(this.getXpath(), { hidden: true, timeout: 60000 })
        ]);
      }
    });
  }

  async asElement(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, { timeout: 1000, visible: true }).then((elemt) => elemt.asElement());
  }
}
