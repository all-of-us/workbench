import {Page} from 'puppeteer';
import Menu from './menu';

export default class TieredMenu extends Menu {

  constructor(page: Page, options: {label?: string, nodeLevel?: number} = {}, selector?: {xpath: string}) {
    super(page, selector);
    options.nodeLevel = options.nodeLevel || 1;
    if (options.label === undefined) {
      this.selector = {xpath: '//*[contains(@class, "p-p-menu-overlay-visible")]'};
    } else {
      this.selector = {xpath: `//*[contains(normalize-space(text()), "${options.label}")]` +
           `/ancestor::node()[${options.nodeLevel}]//*[contains(@class, "p-menu-overlay-visible")]`};
    }
  }

  static async selectMenu(page: Page,
                          menuItem: string | string[],
                          options: {label?: string, ancestorNodeLevel?: number} ={}): Promise<void> {
    const node = await page.waitForXPath(new TieredMenu(page, options).selector.xpath, {visible: true});
    await TieredMenu.selectMenuHelper(node, menuItem);
  }

}
