import Container from 'app/container';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';

export default class Cell extends Container {
  constructor(page: Page, xpath: string, container?: Container) {
    super(page, container === undefined ? xpath : `${container.getXpath()}${xpath}`);
  }

  async getCellValue(): Promise<string> {
    const element = await this.asElement();
    return getPropValue<string>(element, 'innerText');
  }
}
