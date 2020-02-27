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

  public async getWebElement(): Promise<WebElement> {
    if (this.webElement === undefined) {
      const element = await findTextarea(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async getValue(): Promise<unknown> {
    await this.getWebElement();
    return await this.webElement.getProperty("value");
  }

  public async focus(): Promise<WebElement> {
    await this.getWebElement();
    await this.webElement.focus();
    return this.webElement;
  }

}
