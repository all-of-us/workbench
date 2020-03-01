import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import TextOptions from './TextOptions';
import WebElement from './WebElement';
import {findRadioButton} from './xpath-finder';

export default class RadioButton extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(
     textOptions?: TextOptions, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {

    throwErr = throwErr || true;
    try {
      this.element = await findRadioButton(this.page, textOptions, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding RadioButton: "${textOptions}".`);
        throw e;
      }
    }
    return this.element;
  }

  public async isSelected() {
    const is = await this.getProperty('checked');
    return !!is;
  }

  /**
   * Select a RadioButton.
   */
  public async select(): Promise<void> {
    const is = await this.isSelected();
    if (!is) {
      await this.click();
      await this.page.waitFor(500);
    }
  }

  /**
   * Unselect a RadioButton.
   */
  public async unSelect() {
    const is = await this.isSelected();
    if (is) {
      await this.click();
      await this.page.waitFor(500);
    }
  }

}
