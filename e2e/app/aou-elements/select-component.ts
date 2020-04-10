import {Page} from 'puppeteer';
import BaseElement from './base-element';

export default class SelectComponent {

  constructor(private readonly page: Page, private readonly label?: string, private readonly nodeLevel?: number) {
    this.page = page;
    this.label = label || undefined;
    this.nodeLevel = nodeLevel || 1;
  }

  /**
   * Select an option in a Select element.
   * @param textValue
   * @param {number} retries Default retry count is 2.
   */
  async select(textValue: string, retries: number = 2): Promise<string | null> {

    const clickText = async () => {
      await this.open(2);
      const selector = this.dropdownXpath() + `//li[contains(normalize-space(text()), "${textValue}")]`;
      const selectValue = await this.page.waitForXPath(selector, {visible: true});
      const selectElement = new BaseElement(this.page, selectValue);
      const textContent = await selectElement.getTextContent();
      await selectValue.click();
      // need to make sure dropdown is disappeared, so it cannot interfere with clicking on elements below.
      await this.waitUntilDropdownClosed();
      return textContent;
    };

    const clickAndCheck = async () => {
      retries --;
      const selectedValue = await clickText();
      // compare to displayed text in Select textbox
      const displayedValue = await this.getSelectedValue();
      if (selectedValue === displayedValue) {
        return selectedValue; // success
      }
      if (retries === 0) {
        return null;
      }
      return await this.page.waitFor(2000).then(clickAndCheck); // two seconds pause and retry
    };

    return clickAndCheck();
  }

  async getSelectedValue(): Promise<unknown> {
    const selector = this.dropdownXpath() + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    const innerText = await displayedValue.getProperty('innerText');
    return await innerText.jsonValue();
  }

  /**
   * Open Select dropdown.
   * @param {number} retries Retry count. Default is 1.
   */
  private async open(retries: number = 1): Promise<void> {
    const click = async () => {
      retries --;
      const is = await this.isOpen();
      if (!is) {
        await this.toggleOpenClose();
      } else {
        return;
      }
      if (retries === 0) {
        return;
      }
      await this.page.waitFor(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggleOpenClose(): Promise<void> {
    const selector = this.dropdownXpath() + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  private async isOpen() {
    const selector = this.dropdownXpath() +
       '/*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown-panel ")]';
    const panel = await this.page.waitForXPath(selector);
    const classNameString = await (await panel.getProperty('className')).jsonValue();
    const splits = classNameString.toString().split(' ');
    await panel.dispose();
    return splits.includes('p-input-overlay-visible');
  }

  private dropdownXpath(): string {
    if (this.label === undefined) {
      return '//*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown ")]';
    }
    return `//*[contains(normalize-space(text()), "${this.label}")]` +
       `/ancestor::node()[${this.nodeLevel}]//*[contains(concat(" ", normalize-space(@class), " ")," p-dropdown ")]`;
  }

  private async waitUntilDropdownClosed() {
    const xpath = this.dropdownXpath() +
       '/*[contains(concat(" ", normalize-space(@class), " "), " p-input-overlay-visible ")]';
    await this.page.waitForXPath(xpath, {hidden: true}).catch((err) => {
      console.error('Select dropdown is not closed.');
      throw err;
    })
  }

}
