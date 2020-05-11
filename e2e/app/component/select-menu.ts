import {Page} from 'puppeteer';
import BaseElement from 'app/element/base-element';
import menu from './menu';

export default class SelectMenu extends menu {

  constructor(page: Page, options: {label?: string, nodeLevel?: number} = {}, selector?: {xpath: string}) {
    super(page, selector);
    options.nodeLevel = options.nodeLevel || 1;
    if (options.label === undefined) {
      this.selector = {xpath: '//*[contains(@class, "p-dropdown")]'};
    } else {
      this.selector = {xpath: `//*[contains(normalize-space(text()), "${options.label}")]` +
           `/ancestor::node()[${options.nodeLevel}]//*[contains(@class, "p-dropdown")]`};
    }
  }

  /**
   * Select an option in a Select element.
   * @param {string} textValue Partial or full string to click in Select.
   * @param {number} maxAttempts Default try count is 2.
   */
  async select(textValue: string, maxAttempts: number = 2): Promise<void> {

    const clickText = async () => {
      await this.open(2);
      const selector = this.selector.xpath + `//li[contains(normalize-space(text()), "${textValue}")]`;
      const selectValue = await this.page.waitForXPath(selector, {visible: true});
      const baseElement = new BaseElement(this.page, selectValue);
      const textContent = await baseElement.getTextContent(); // get full text
      await selectValue.click();
      // need to make sure dropdown is disappeared, so it cannot interfere with clicking on elements below.
      await this.waitUntilDropdownClosed();
      return textContent;
    };

    const clickAndCheck = async () => {
      maxAttempts--;
      const shouldSelectedValue = await clickText();
      // compare to displayed text in Select textbox
      const displayedValue = await this.getSelectedValue();
      if (shouldSelectedValue === displayedValue) {
        return shouldSelectedValue; // success
      }
      if (maxAttempts <= 0) {
        return null;
      }
      return await this.page.waitFor(2000).then(clickAndCheck); // two seconds pause and retry
    };

    await clickAndCheck();
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedValue(): Promise<unknown> {
    const selector = this.selector.xpath + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    const innerText = await displayedValue.getProperty('innerText');
    return await innerText.jsonValue();
  }

  /**
   * Open Select dropdown.
   * @param {number} maxAttempts Default is 1.
   */
  private async open(maxAttempts: number = 1): Promise<void> {
    const click = async () => {
      await this.toggleOpenClose();
      const opened = await this.isOpen();
      if (opened) {
        return;
      }
      if (maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitFor(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggleOpenClose(): Promise<void> {
    const selector = this.selector.xpath + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  private async isOpen() {
    const selector = this.selector.xpath +
       '/*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown-panel ")]';
    try {
      const panel = await this.page.waitForXPath(selector);
      const classNameString = await (await panel.getProperty('className')).jsonValue();
      const splits = classNameString.toString().split(' ');
      await panel.dispose();
      return splits.includes('p-input-overlay-visible');
    } catch (err) {
      return false;
    }
  }

  private async waitUntilDropdownClosed() {
    const xpath = this.selector.xpath + '/*[contains(@class), "p-input-overlay-visible")]';
    await this.page.waitForXPath(xpath, {hidden: true}).catch((err) => {
      console.error('Select dropdown is not closed.');
      throw err;
    })
  }

}
