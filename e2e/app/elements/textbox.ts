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

  public async getWebElement(): Promise<WebElement> {
    if (this.webElement === undefined) {
      const element = await findTextbox(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async focus(): Promise<WebElement> {
    const input = await this.getWebElement();
    await input.focus();
    return input;
  }

  public async type(inputValue: string): Promise<void> {
    const input = await this.getWebElement();
    await input.focus();
    await input.type(inputValue);
  }

  public async getValue(): Promise<unknown> {
    return await (await this.getWebElement()).getProperty("value");
  }

}
