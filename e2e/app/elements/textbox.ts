import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findTextbox} from './xpath-finder';

export default class Textbox {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (!!this.webElement) {
      const element = await findTextbox(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async focus(): Promise<void> {
    const input = await this.get();
    await input.focus();
  }

  public async type(inputValue: string): Promise<void> {
    const input = await this.get();
    await input.focus();
    await input.type(inputValue);
  }

  public async getValue(): Promise<unknown> {
    return (await this.get()).getProperty("value");
  }

}
