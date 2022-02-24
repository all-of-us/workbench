import Container from 'app/container';
import { ElementHandle, Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';

export const enum CellContent {
  SVG = './/*[local-name()="svg"]'
}
export default class Cell extends Container {
  constructor(page: Page, xpath: string, container?: Container) {
    super(page, container === undefined ? xpath : `${container.getXpath()}${xpath}`);
  }

  async getText(): Promise<string> {
    const td = await this.asElement();
    return getPropValue<string>(td, 'innerText');
  }

  async getContent(cellContent: CellContent): Promise<ElementHandle[]> {
    const td = await this.asElement();
    return await td.$x(cellContent);
  }
}
