import {Page} from 'puppeteer';
import BaseElement from 'app/element/base-element';
import Container from 'app/element/container';

export default class SelectMenu {

  private readonly puppeteerPage: Page;
  private readonly containerXpath: string;
  private readonly labelName: string;
  private readonly ancestorNodeLevel: number;

  constructor(pageOptions: {puppeteerPage: Page, container?: Container}, labelOptions: { label?: string, nodeLevel?: number} = {}) {
    this.puppeteerPage = pageOptions.puppeteerPage;
    this.containerXpath = (pageOptions.container === undefined) ? '' : pageOptions.container.getXpath();
    this.labelName = labelOptions.label || undefined;
    this.ancestorNodeLevel = labelOptions.nodeLevel || 1;
  }

  /**
   * Select an option in a Select element.
   * @param {string} textValue Partial or full string to click in Select.
   * @param {number} maxAttempts Default try count is 2.
   */
  async select(textValue: string, maxAttempts: number = 2): Promise<void> {
    const clickText = async () => {
      await this.open(2);
      const selector = this.dropdownXpath() + `//li[contains(normalize-space(text()), "${textValue}")]`;
      const selectValue = await this.puppeteerPage.waitForXPath(selector, {visible: true});
      const baseElement = BaseElement.asBaseElement(this.puppeteerPage, selectValue);
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
      return await this.puppeteerPage.waitFor(2000).then(clickAndCheck); // two seconds pause and retry
    };

    await clickAndCheck();
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedValue(): Promise<unknown> {
    const selector = this.dropdownXpath() + '/label';
    const displayedValue = await this.puppeteerPage.waitForXPath(selector, { visible: true });
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
      await this.puppeteerPage.waitFor(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggleOpenClose(): Promise<void> {
    const selector = this.dropdownXpath() + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.puppeteerPage.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  private async isOpen() {
    const selector = this.dropdownXpath() +
       '/*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown-panel ")]';
    try {
      const panel = await this.puppeteerPage.waitForXPath(selector);
      const classNameString = await (await panel.getProperty('className')).jsonValue();
      const splits = classNameString.toString().split(' ');
      await panel.dispose();
      return splits.includes('p-input-overlay-visible');
    } catch (err) {
      return false;
    }
  }

  private dropdownXpath(): string {
    const containsString = '//*[contains(concat(" ", normalize-space(@class), " ")," p-dropdown ")]';
    if (this.labelName === undefined) {
      return `${this.containerXpath}${containsString}`;
    }
    return `${this.containerXpath}//*[contains(normalize-space(text()), "${this.labelName}")]` +
       `/ancestor::node()[${this.ancestorNodeLevel}]${containsString}`;
  }

  private async waitUntilDropdownClosed() {
    const xpath = this.dropdownXpath() +
       '/*[contains(concat(" ", normalize-space(@class), " "), " p-input-overlay-visible ")]';
    await this.puppeteerPage.waitForXPath(xpath, {hidden: true}).catch((err) => {
      console.error('Select dropdown is not closed.');
      throw err;
    })
  }

}
