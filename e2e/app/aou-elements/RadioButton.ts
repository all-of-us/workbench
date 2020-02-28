import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findRadioButton} from './xpath-finder';

export default class RadioButton extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementLabel: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.labelText = aElementLabel;
    throwErr = throwErr || true;
    try {
      this.element = await findRadioButton(this.page, this.labelText, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding RadioButton: "${this.labelText}".`);
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
    }
  }

  /**
   * Unselect a RadioButton.
   */
  public async unSelect() {
    const is = await this.isSelected();
    if (is) {
      await this.click();
    }
  }

}
