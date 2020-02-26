import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findRadioButton} from './xpath-finder';

export default class RadioButton {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (!!this.webElement) {
      const element = await findRadioButton(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async isSelected() {
    const propChecked = (await this.get()).getProperty('checked');
    return !!propChecked;
  }

  /**
   * Select a Radio button.
   */
  public async select(): Promise<void> {
    const isChecked = await this.isSelected();
    if (!isChecked) {
      (await this.get()).click();
    }
  }

  /**
   * Unselect a Radio button.
   */
  public async unSelect() {
    const isChecked = await this.isSelected();
    if (isChecked) {
      (await this.get()).click();
    }
  }


}
