import * as fp from 'lodash/fp';
import {ElementHandle, Page} from 'puppeteer';
import {WorkspaceAccessLevel} from 'app/text-labels';
import {getPropValue} from 'utils/element-utils';
import BaseElement from './base-element';
import Textbox from './textbox';

export default class ReactSelect extends BaseElement {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async waitForInput(label: string): Promise<Textbox> {
    return Textbox.findByName(this.page, {containsText: label, ancestorLevel: 1});
  }

  async selectOption(level: WorkspaceAccessLevel): Promise<void> {
    const option = await this.waitForOption(level);
    await option.click({delay: 20});
  }

  async waitForOption(level: WorkspaceAccessLevel): Promise<ElementHandle> {
     // The label in the select menu uses title case.
    const levelText =  level[0].toUpperCase() + level.substring(1).toLowerCase();
    const selector = `//*[starts-with(@id,"react-select") and text()="${levelText}"]`;
    return this.page.waitForXPath(selector, {visible: true});
  }

  async getAllOptionsText(): Promise<string[]> {
    const selector = `//*[starts-with(@id,"react-select") and text()]`;
    await this.page.waitForXPath(selector, {visible: true});
    const elements = await this.page.$x(selector);
    return fp.flow(
       fp.map( async (elem: ElementHandle) => (await getPropValue<string>(elem, 'textContent')).trim()),
       contents => Promise.all(contents)
    )(elements);
  }

}
