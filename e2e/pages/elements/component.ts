import {ElementHandle, JSHandle} from 'puppeteer';import {Page} from 'puppeteer';
import Widget from './widget';

export default class Component extends Widget {
   // $x("//*[child::*/label[normalize-space(text())='Disease-focused research']]//input[@type='text']"
   // $x("//*[child::*/label[normalize-space(text())='Disease-focused research']]//input[@type='checkbox']")
   // $x("//*[child::*/label[normalize-space(text())='Other Purpose']]//textarea")

  public label: string;

  constructor(page: Page, label: string) {
    super(page);
    this.label = label;
  }

  public async getCheckbox(): Promise<ElementHandle> {
    const selectr = this.makeXpath() + '//input[@type=\'checkbox\']';
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true})
  }

  public async getInput(): Promise<ElementHandle> {
    const selectr = this.makeXpath() + '//input[@type=\'text\']';
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true})
  }

  public async getTextarea(): Promise<ElementHandle> {
    const selectr = this.makeXpath() + '//textarea';
    return await this.puppeteerPage.waitForXPath(selectr, {visible: true})
  }

  private makeXpath(): string {
    return `//*[child::*/label[normalize-space(text())="${this.label}"]]`;
  }

}