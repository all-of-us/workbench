import {Page} from 'puppeteer-core';
import {getAttribute, getProperty} from '../../services/element-handler';
import * as elementHandler from '../../services/element-handler';
import Widget from './widget';

export default class Radio extends Widget {
  public label: string;

  constructor(page: Page, label: string) {
    super(page);
    this.label = label;
  }

  public async get() {
    return await this.findRadio(this.label);
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

  public async isChecked() {
    const attrChecked = await getProperty(this.puppeteerPage, await this.get(), 'checked');
    return attrChecked !== null && attrChecked === true;
  }

}
