import BaseElement from './base-element';
import { Page } from 'puppeteer';
import { ElementType, XPathOptions } from 'app/xpath-options';
import Container from 'app/container';
import { buildXPath } from 'app/xpath-builders';
import { getPropValue } from 'utils/element-utils';

export default class Switch extends BaseElement {
  static findByName(page: Page, xOpt: XPathOptions, container?: Container): Switch {
    xOpt.type = ElementType.Switch;
    const xpath = buildXPath(xOpt, container);
    return new Switch(page, xpath);
  }

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async getLabel(): Promise<string> {
    const labelElement = await this.asElementHandle();
    return getPropValue<string>(labelElement, 'innerText');
  }

  async toggle(): Promise<void> {
    await this.click();
  }

  async turnOff(): Promise<void> {
    const checked = await this.isOn();
    if (checked) {
      await this.toggle();
    }
  }

  async turnOn(): Promise<void> {
    const checked = await this.isOn();
    if (!checked) {
      await this.toggle();
    }
  }

  async isOn(): Promise<boolean> {
    const labelElement = await this.asElementHandle();
    const [input] = await labelElement.$x('.//input[@type="checkbox"]');
    return !!(await getPropValue<boolean>(input, 'checked'));
  }
}
