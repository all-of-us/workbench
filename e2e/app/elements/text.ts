import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findText} from './xpath-finder';

export default class Text {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (!!this.webElement) {
      const element = await findText(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }


}
