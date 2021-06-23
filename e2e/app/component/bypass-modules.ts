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

  async verifyAllModulesBypassed(): Promise<Page> {
    await this.verifyTrainingBypass();
    await this.verifyEraCommBypass();
    await this.verifyTwoFABypass();
    await this.verifyDUCCBypass();
    await this.clickCancelBypass();
    return page;
  }

  /**
   * Toggle checkbox state.
   * @param {boolean} checked
   */

  async verifyTrainingBypass(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='Compliance Training']/preceding-sibling::div/input[@type='checkbox']`;
    const checkbox = new Checkbox(this.page, xpath);
    expect(await checkbox.isChecked()).toBe(true);
    return true;
  }

  async verifyEraCommBypass(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='eRA Commons Linking']/preceding-sibling::div/input[@type='checkbox']`;
    const checkbox = new Checkbox(this.page, xpath);
    expect(await checkbox.isChecked()).toBe(true);
    return true;
  }

  async verifyTwoFABypass(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='Two Factor Auth']/preceding-sibling::div/input[@type='checkbox']`;
    const checkbox = new Checkbox(this.page, xpath);
    expect(await checkbox.isChecked()).toBe(true);
    return true;
  }

  async verifyDUCCBypass(): Promise<boolean> {
    const xpath = `${this.getXpath()}//label/span[text()='Data Use Agreement']/preceding-sibling::div/input[@type='checkbox']`;
    const checkbox = new Checkbox(this.page, xpath);
    expect(await checkbox.isChecked()).toBe(true);
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
