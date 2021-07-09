import { ElementHandle, Page } from 'puppeteer';
import { waitWhileLoading } from 'utils/waits-utils';
import * as fp from 'lodash/fp';
import { LinkText } from 'app/text-labels';
import Button from 'app/element/button';

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

  async waitUntilVisible(timeout = 60000): Promise<void> {
    await Promise.all([
      waitWhileLoading(this.page, timeout),
      this.page.waitForXPath(this.getXpath(), { visible: true, timeout })
    ]);
  }

  async waitUntilClose(timeout = 2 * 60 * 1000): Promise<void> {
    await Promise.all([
      waitWhileLoading(this.page, timeout),
      this.page.waitForXPath(this.getXpath(), { hidden: true, timeout })
    ]);
  }

  /**
   * Click a button.
   * @param {string} buttonLabel The button text label.
   * @param waitOptions Wait for navigation or/and modal close after click button with a timeout.
   */
  async clickButton(
    buttonLabel: LinkText,
    waitOptions: { waitForNav?: boolean; waitForClose?: boolean; timeout?: number } = {}
  ): Promise<void> {
    const { waitForNav = false, waitForClose = false, timeout } = waitOptions;

    const button = Button.findByName(this.page, { normalizeSpace: buttonLabel }, this);
    await button.waitUntilEnabled();
    await button.focus();

    await Promise.all(
      fp.flow(
        fp.filter<{ shouldWait: boolean; waitFn: () => Promise<void> }>('shouldWait'),
        fp.map((item) => item.waitFn()),
        fp.concat([button.click({ delay: 10 }), waitWhileLoading(this.page)])
      )([
        {
          shouldWait: waitForNav,
          waitFn: async () => {
            await this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'], timeout });
          }
        },
        {
          shouldWait: waitForClose,
          waitFn: async () => {
            await this.waitUntilClose(timeout);
          }
        }
      ])
    );
  }

  async asElement(): Promise<ElementHandle | null> {
    return this.page.waitForXPath(this.xpath, { timeout: 1000, visible: true }).then((elemt) => elemt.asElement());
  }
}
