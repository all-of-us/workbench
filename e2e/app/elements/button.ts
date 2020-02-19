import {Page} from 'puppeteer';
import * as elementHandler from '../../driver/elementHandle-util';
import Widget from './widget';

export default class Button extends Widget {
  public label: string;

  constructor(page: Page, label: string) {
    super(page);
    this.label = label;
  }

  public async get() {
    return await this.findButton(this.label);
  }

  public async click() {
    const button = await this.get();
    await button.click();
  }

  public async getText() {
    const button = await this.get();
    return await elementHandler.getTextContent(this.puppeteerPage, button);
  }

  public async isDisabled() {
    // TODO
  }
}
