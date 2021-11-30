import { Page } from 'puppeteer';
import BaseMenu from './base-menu';
import Checkbox from 'app/element/checkbox';
import { getPropValue } from 'utils/element-utils';

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

  // xpath of the bypass-link-checkbox in user admin table
  getMenuItemXpath(accessModule: string): string {
    return `${this.getXpath()}//span[text()= "${accessModule}"]/preceding-sibling::div/input[@type='checkbox']`;
  }

  //get status of each bypass access modules
  async getAccessModuleStatus(accessModule: string): Promise<boolean> {
    const selector = this.getMenuItemXpath(accessModule);
    const btn = new Checkbox(this.page, selector);
    const accessModuleStatus = await getPropValue<boolean>(await btn.asElementHandle(), 'checked');
    return accessModuleStatus;
  }

  // get the module name and the status of each modules in test env
  async getBypassModuleTest(): Promise<boolean[]> {
    const bypassAdminTest = [
      'RT Compliance Training',
      'eRA Commons Linking',
      'Two Factor Auth',
      'Data User Code of Conduct',
      'RAS Login.gov Link'
    ];
    const moduleNames: string[] = [];
    let i: number;
    const btnStatus: boolean[] = [];
    for (i = 0; i < bypassAdminTest.length; i++) {
      moduleNames.push(bypassAdminTest[i]);
      btnStatus.push(await this.getAccessModuleStatus(moduleNames[i]));
    }
    return btnStatus;
  }

  // get the module name and the status of each modules in staging env
  async getBypassModuleStaging(): Promise<boolean[]> {
    const bypassAdminStaging = ['eRA Commons Linking', 'Two Factor Auth', 'Data User Code of Conduct'];
    const moduleNames: string[] = [];
    let i: number;
    const btnStatus: boolean[] = [];
    for (i = 0; i < bypassAdminStaging.length; i++) {
      moduleNames.push(bypassAdminStaging[i]);
      btnStatus.push(await this.getAccessModuleStatus(moduleNames[i]));
    }
    return btnStatus;
  }

  // check the env and run the respective function to get the module status
  async getBypassAdminEnv(): Promise<boolean[]> {
    const checkEnv = process.env.WORKBENCH_ENV;
    switch (checkEnv) {
      case 'test':
        return this.getBypassModuleTest();
      case 'staging':
        return this.getBypassModuleStaging();
    }
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
}
