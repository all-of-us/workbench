import { Page } from 'puppeteer';
import BaseMenu from './base-menu';
import Checkbox from 'app/element/checkbox';

const defaultXpath = '//*[@id="popup-root"]';

export default class BypassPopup extends BaseMenu {
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

  getTrainingBypassToggle(): Checkbox {
    const xpath = `${this.getXpath()}//label/span[text()='Compliance Training']/preceding-sibling::div/input[@type='checkbox']`;
    return new Checkbox(this.page, xpath);
  }

  getEraCommBypassToggle(): Checkbox {
    const xpath = `${this.getXpath()}//label/span[text()='eRA Commons Linking']/preceding-sibling::div/input[@type='checkbox']`;
    return new Checkbox(this.page, xpath);
  }

  getTwoFABypassToggle(): Checkbox {
    const xpath = `${this.getXpath()}//label/span[text()='Two Factor Auth']/preceding-sibling::div/input[@type='checkbox']`;
    return new Checkbox(this.page, xpath);
  }

  getDUCCBypassToggle(): Checkbox {
    const xpath = `${this.getXpath()}//label/span[text()='Data Use Agreement']/preceding-sibling::div/input[@type='checkbox']`;
    return new Checkbox(this.page, xpath);
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
