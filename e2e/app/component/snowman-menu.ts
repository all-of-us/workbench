import {Page} from 'puppeteer';
import {Option} from 'app/text-labels';
import Link from 'app/element/link';
import {getPropValue} from 'utils/element-utils';
import Container from 'app/container';

export const snowmanIconXpath = '//clr-icon[@shape="ellipsis-vertical"]';
const defaultXpath = '//*[@id="popup-root"]';

export default class SnowmanMenu extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

   /**
    *  Get texts of all visible options.
    */
  async getAllOptionTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}//*[@role='button']/text()`;
    await this.page.waitForXPath(selector, {visible: true});
    const elements = await this.page.$x(selector);
    const actionTextsArray = [];
    for (const elem of elements) {
      actionTextsArray.push(await getPropValue<string>(elem, 'textContent'));
      await elem.dispose();
    }
    return actionTextsArray;
  }

  /**
   * Select an option in snowman menu.
   * @param {Option} option
   * @param opt
   */
  async select(option: Option, opt: { waitForNav?: boolean } = {}): Promise<void> {
    const { waitForNav = true } = opt;
    const link = this.findOptionLink(option);
    // Triggers page navigation event.
    if (waitForNav) {
      await Promise.all([
        this.page.waitForNavigation({waitUntil: ['load', 'networkidle0']}),
        link.click(),
      ]);
    } else {
      await link.click();
    }
    await link.dispose();
  }

  /**
   * Determine if an option in snowman menu is disabled.
   * @param {Option} option Snowman menuitem.
   */
  async isOptionDisabled(option: Option): Promise<boolean> {
    const link = this.findOptionLink(option);
    const cursorNotAllowed = await link.isCursorNotAllowed();
    await link.dispose();
    return cursorNotAllowed;
  }

  findOptionLink(action: Option): Link {
    const selector = `${this.getXpath()}//*[@role='button' and text()='${action}']`;
    return new Link(this.page, selector);
  }

}
