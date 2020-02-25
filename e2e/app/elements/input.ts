import {ElementHandle, Page} from 'puppeteer';
import * as elementHandler from '../../driver/elementHandle-util';
import Widget from './widget';

export default class Input extends Widget {
  public label: string;
  public elemHandler: ElementHandle;

  constructor(page: Page, label: string) {
    super(page);
    this.label = label;
  }

  public async get() {
    return await this.findInput(this.label);
  }

  public async focus() {
    const input = await this.get();
    await input.focus();
  }

  public async type(inputValue: string) {
    const input = await this.get();
    await input.focus();
    await input.type(inputValue);
  }

  public async getValue() {
    const input = await this.get();
    return await elementHandler.getValue(this.puppeteerPage, input);
  }

  public async isDisabled() {
    const attrChecked = await elementHandler.getProperty(this.puppeteerPage, await this.get(), 'disabled');
    return attrChecked !== null && attrChecked === true;
  }


}

export class InputElement {
  private readonly element: ElementHandle;

  constructor(elemHandler: ElementHandle) {
    this.element = elemHandler;
  }

  public async focus() {
    await this.element.focus();
  }

  public async type(inputTexts: string) {
    await this.element.focus();
    await this.element.type(inputTexts);
  }

  public async getValue() {
    this.element.getProperty("value")
    return await elementHandler.getValue(this.puppeteerPage, input);
  }

  public async isDisabled() {
    const attrChecked = await elementHandler.getProperty(this.puppeteerPage, await this.get(), 'disabled');
    return attrChecked !== null && attrChecked === true;
  }

}
