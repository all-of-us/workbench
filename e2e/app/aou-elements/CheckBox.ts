import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findCheckBox, findText} from './xpath-finder';

export default class CheckBox extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findCheckBox(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding CheckBox: "${this.name}".`);
        throw e;
      }
    }
    return this.element;
  }

  /**
   * Checked means element does not have a `checked` property
   */
  public async isChecked(): Promise<boolean> {
    const is = await this.getProperty('checked');
    return !!is;
  }

  /**
   * Click on checkbox element for checked
   */
  public async check(): Promise<void> {
    const is = await this.isChecked();
    if (!is) {
      // click on text will check the checkbox
      const txt = await this.text();
      await txt.click();
    }
  }

  /**
   * Click on checkbox element for unchecked
   */
  public async unCheck() {
    const is = await this.isChecked();
    if (!is) {
      // click on text will uncheck the checkbox
      const txt = await this.text();
      await txt.click();
    }
  }

  private async text(): Promise<ElementHandle> {
    return await findText(this.page, this.name);
  }

}
