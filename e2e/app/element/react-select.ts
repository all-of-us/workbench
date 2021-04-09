import * as fp from 'lodash/fp';
import { ElementHandle, Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import BaseElement from './base-element';
import Textbox from './textbox';

export default class ReactSelect extends BaseElement {
  private readonly name: string;

  constructor(page: Page, opts: { xpath?: string; name: string }) {
    super(page, opts.xpath);
    this.name = opts.name;
  }

  waitForInput(): Textbox {
    const selector = `${this.getRootXpath()}//input`;
    return new Textbox(this.page, selector);
  }

  async selectOption(optionText: string): Promise<void> {
    const input = this.waitForInput();
    await input.click({ delay: 20 });
    const option = await this.waitForOption(optionText);
    await option.click({ delay: 20 });
  }

  async waitForOption(optionText: string): Promise<ElementHandle> {
    const selector = this.getOptionXpath(optionText);
    return this.page.waitForXPath(selector, { visible: true });
  }

  async getAllOptionTexts(): Promise<string[]> {
    const selector = this.getOptionXpath();
    await this.page.waitForXPath(selector, { visible: true });
    const elements = await this.page.$x(selector);
    return fp.flow(
      fp.map(async (elem: ElementHandle) => (await getPropValue<string>(elem, 'textContent')).trim()),
      (contents) => Promise.all(contents)
    )(elements);
  }

  async getSelectedOption(): Promise<string> {
    const selector = `${this.getRootXpath()}//*[contains(@class, "-singleValue")]/text()`;
    const value = await this.page.waitForXPath(selector, { visible: true });
    return getPropValue<string>(value, 'textContent');
  }

  private getOptionXpath(optionText?: string): string {
    if (optionText) {
      return `${this.getRootXpath()}//*[starts-with(@id,"react-select") and text()="${optionText}"]`;
    }
    return `${this.getRootXpath()}//*[starts-with(@id,"react-select") and text()]`;
  }

  private getRootXpath(): string {
    return `//*[contains(text(),"${this.name}")]/following-sibling::*`;
  }
}
