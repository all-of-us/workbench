import {Page} from 'puppeteer';
import WebElement from './web-element';
import {findButton} from './xpath-finder';

export default class Button {

  private readonly label: string;
  private readonly page: Page;
  private webElement: WebElement;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async get(): Promise<WebElement> {
    if (this.webElement === undefined) {
      const element = await findButton(this.page, this.label);
      this.webElement = new WebElement(element);
    }
    return this.webElement;
  }

  public async getButtonLabel(): Promise<string> {
    return await (await this.get()).getTextContent();
  }

   /**
    * Checking style 'cursor' value.
    */
  public async isDisabled(): Promise<boolean> {
    const cursor = await (await this.get()).getComputedStyle("cursor");
    return cursor === 'not-allowed';
  }


}
