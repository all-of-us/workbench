import { Page } from 'puppeteer';
import BaseMenu from './base-menu';
import Checkbox from 'app/element/checkbox';

const defaultXpath = '//*[@id="popup-root"]';

export default class BypassPopup extends BaseMenu {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   *  Get labels of all modules.
   */
  async getAllToggleTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}//label/span/text()`;
    return this.getMenuItemTexts(selector);
  }

  /**
   *  Get all toggle status of all modules.
   */
  async getAllToggleInputs(): Promise<boolean[]> {
    const selector = `${this.getXpath()}//label/div/input[@type='checkbox']`;
    return this.getToggleStatus(selector);
  }

  // xpath of the bypass-link-checkbox in user admin table
  getMenuItemXpath(toggleAccessText: string): string {
    return `${this.getXpath()}//span[text()= "${toggleAccessText}"]/preceding-sibling::div/input[@type='checkbox']`;
  }

  //get status of each bypass modules individually
  async getEachModuleStatus(toggleAccessText: string): Promise<boolean> {
    const selector = this.getMenuItemXpath(toggleAccessText);
    return this.getEachToggleStatus(selector);
  }

  // click the checkmark on the bypass-popup to save the bypass access
  async clickCheckmarkBypass(): Promise<void> {
    const xpath = `${this.getXpath()}//*[local-name()='svg' and @data-icon='check']`;
    await new Checkbox(this.page, xpath).click();
  }

  // click the cancel icon to close the bypass-popup
  async clickCancelBypass(): Promise<void> {
    const xpath = `${this.getXpath()}//*[local-name()='svg' and @data-icon='times']`;
    await new Checkbox(this.page, xpath).click();
  }

  async waitUntilGone(page: Page, timeout = 60000): Promise<void> {
    await page.waitForXPath(this.xpath, { hidden: true, timeout });
  }
}
