import {ElementHandle, Page, WaitForSelectorOptions} from 'puppeteer';
import WebElement from './WebElement';
import {findRadioButton} from './xpath-finder';

export default class RadioButton extends WebElement {

  constructor(aPage: Page) {
    super(aPage);
  }
   
  public async withLabel(aElementName: string, options?: WaitForSelectorOptions, throwErr?: boolean): Promise<ElementHandle> {
    this.name = aElementName;
    throwErr = throwErr || true;
    try {
      this.element = await findRadioButton(this.page, this.name, options);
    } catch (e) {
      if (throwErr) {
        console.error(`FAILED finding RadioButton: "${this.name}".`);
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
