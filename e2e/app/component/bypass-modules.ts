import { Page } from 'puppeteer';
import BaseMenu from './base-menu';
import Checkbox from 'app/element/checkbox';

const defaultXpath = '//*[@id="popup-root"]';

export default class BypassLink extends BaseMenu {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  /**
   *  Get texts of all visible options.
   */
  async getAllToggleTexts(): Promise<string[]> {
    const selector = `${this.getXpath()}//label/span/text()`;
    return this.getMenuItemTexts(selector);
  }

  getMenuItemXpath(toggleAccessText: string): string {
    return `//*[@role="switch" and normalize-space()="${toggleAccessText}"]`;
  }

  async bypassAllModules(): Promise<Page> {
    await this.getTrainingToggle();
    await this.getEraCommToggle();
    await this.getTwoFAToggle();
    await this.getDUCCToggle();
    await this.clickCheckmarkBypass();
    await this.waitUntilGone(page);
    return page;
  }

  /**
   * Toggle checkbox state.
   * @param {boolean} checked
   */
  async getTrainingToggle(): Promise<void> {
    const xpath = `${this.getXpath()}//label/span[text()='Compliance Training']`;
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.check();
  }

  async getEraCommToggle(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='eRA Commons Linking']`;
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.toggle(true);
    return true;
  }

  async getTwoFAToggle(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='Two Factor Auth']`;
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.toggle(true);
    return true;
  }

  async getDUCCToggle(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='Data Use Agreement']`;
    const checkbox = new Checkbox(this.page, xpath);
    await checkbox.toggle(true);
    return true;
  }

  // click the checkmark on the bypass module to save the bypass access
  async clickCheckmarkBypass(): Promise<void> {
    const xpath = `${this.getXpath()}//*[local-name()='svg' and @data-icon='check']`;
    const button = new Checkbox(this.page, xpath);
    await button.click();
  }

  // click the cancel icon to close the bypass module
  async clickCancelBypass(): Promise<void> {
    const xpath = `${this.getXpath()}//*[local-name()='svg' and @data-icon='times']`;
    const button = new Checkbox(this.page, xpath);
    await button.click();
  }

  async waitUntilGone(page: Page, timeout = 60000): Promise<void> {
    await page.waitForXPath(this.xpath, { hidden: true, timeout });
  }
}
