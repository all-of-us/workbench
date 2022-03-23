import { ElementHandle, Page } from 'puppeteer';
import { waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';
import Button from 'app/element/button';
import { exists } from 'utils/element-utils';

/**
 * This is the super base class.
 * Every element needs a Page object and a xpath for locating the element.
 */
export default class Container {
  constructor(protected readonly page: Page, protected xpath?: string) {}

  getXpath(): string | null {
    return this.xpath?.length > 0 ? this.xpath : null;
  }

  setXpath(xpath: string): void {
    this.xpath = xpath;
  }

  async isVisible(timeout = 1000): Promise<boolean> {
    return exists(this.page, this.getXpath(), { timeout });
  }

  async waitUntilVisible(timeout = 60000): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), { visible: true, timeout });
    await waitWhileLoading(this.page, { timeout });
  }

  async waitUntilClose(timeout = 2 * 60 * 1000): Promise<void> {
    await this.page.waitForXPath(this.getXpath(), { hidden: true, visible: false, timeout });
  }

  /**
   * Click a button.
   * @param {string} buttonLabel The button text label.
   * @param waitOptions Wait for navigation or/and modal close after click button with a timeout.
   */
  async clickButton(
    buttonLabel: LinkText,
    waitOptions: {
      waitForClose?: boolean;
      timeout?: number;
      waitForLoadingSpinner?: boolean;
    } = {}
  ): Promise<void> {
    const { waitForClose = false, timeout, waitForLoadingSpinner = true } = waitOptions;

    const button = await this.findButton(buttonLabel);
    if (waitForClose) {
      const waitForClosePromise = this.waitUntilClose(timeout);
      await button.click({ delay: 10 });
      await waitForClosePromise;
    } else {
      await button.click({ delay: 10 });
    }
    if (waitForLoadingSpinner) {
      await waitWhileLoading(this.page);
    }
  }

  async asElement(): Promise<ElementHandle | null> {
    return this.page.waitForXPath(this.xpath, { timeout: 1000, visible: true }).then((elemt) => elemt.asElement());
  }

  async findButton(buttonLabel: LinkText | string): Promise<Button> {
    const button = Button.findByName(this.page, { normalizeSpace: buttonLabel }, this);
    await button.waitUntilEnabled();
    return button;
  }
}
