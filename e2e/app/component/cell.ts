import Container from 'app/container';
import { Page } from 'puppeteer';

export default class Cell extends Container {
  constructor(page: Page, xpath: string, container?: Container) {
    super(page, container === undefined ? xpath : `${container.getXpath()}${xpath}`);
  }
}
