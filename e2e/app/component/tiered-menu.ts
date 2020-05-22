import Container from 'app/container';
import {Page} from 'puppeteer';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " p-tieredmenu ")]';

export default class TieredMenu extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

}
