import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findTextarea} from './xpath-finder';

export default class TextArea {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (!!this.webElement) {
      const element = await findTextarea(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async getValue(): Promise<unknown> {
    return (await this.get()).getProperty("value");
  }

}
