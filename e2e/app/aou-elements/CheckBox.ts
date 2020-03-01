import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findCheckBox} from './xpath-finder';

export default class CheckBox extends WebElement {

  protected textOptions: TextOptions;

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, waitOptions?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    this.textOptions = textOptions;
    throwErr = throwErr || true;
    try {
      this.element = await findCheckBox(this.page, textOptions, waitOptions);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding CheckBox: "${this.textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

  // find checkbox label by matching label
  public async withMatchLabel(checkboxLabel: string): Promise<ElementHandle> {
    const selector = '[type="checkbox"] + label';
    await this.page.waitForSelector(selector);
    const handles = await this.page.$$(selector);
    for (const elem of handles) {
      const innerTxt  = await (await elem.getProperty('innerText')).jsonValue();
      if (innerTxt === checkboxLabel) {
        this.element = elem;
        return elem;
      }
    }
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
      await this.focus();
      await this.clickWithEval();
      await this.page.waitFor(500);
    }
    // check and retry ??
  }

  /**
   * Click on checkbox element for unchecked
   */
  public async unCheck() {
    const is = await this.isChecked();
    if (!is) {
      await this.focus();
      await this.clickWithEval();
      await this.page.waitFor(500);
    }
  }

}
