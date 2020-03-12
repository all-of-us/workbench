import {Page} from 'puppeteer';

export default class SelectComponent {

  constructor(private readonly page: Page, private readonly label?: string) {
    this.page = page;
    this.label = label || undefined;
  }

  async select(textValue: string) {
    await this.open(2); // with 2 retries
    const selector = this.componentXpath() + `//li[@class='p-dropdown-item'][normalize-space(.)="${textValue}"]`;
    const selectValue = await this.page.waitForXPath(selector, { visible: true });
    await selectValue.click();
    // need to make sure dropdown is disappeared, so it cannot interfere with clicking on elements below.
    await this.waitUntilDropdownClosed();
  }

  async getSelectedValue(): Promise<unknown> {
    const selector = this.componentXpath() + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    const innerText = await displayedValue.getProperty('innerText');
    return await innerText.jsonValue();
  }

  // open Select dropdown with retries
  async open(retries: number): Promise<void> {
    const click = async () => {
      const is = await this.isOpen();
      if (!is) {
        await this.toggleOpenClose();
      } else {
        return;
      }
      if (retries < 0) {
        return;
      }
      retries --;
      await this.page.waitFor(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggleOpenClose(): Promise<void> {
    const selector = this.componentXpath() + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  private async isOpen() {
    const selector = this.componentXpath() + '/*[contains(normalize-space(@class),"p-dropdown-panel")]';
    const panel = await this.page.waitForXPath(selector);
    const classNameString = await (await panel.getProperty('className')).jsonValue();
    const splits = classNameString.toString().split(' ');
    await panel.dispose();
    return splits.includes('p-input-overlay-visible');
  }

  private componentXpath(): string {
    if (this.label === undefined) {
      return '//*[contains(normalize-space(@class),"p-dropdown")]';
    }
    return `//*[child::*[normalize-space()="${this.label}"]]/*[contains(normalize-space(@class),"p-dropdown")]`;
  }

  private async waitUntilDropdownClosed() {
    const xpath = this.componentXpath() + '/*[contains(normalize-space(@class), "p-input-overlay-visible")]';
    await this.page.waitForXPath(xpath, {hidden: true}).catch((err) => {
      console.error('Select dropdown is not closed.');
      throw err;
    })
  }

}
