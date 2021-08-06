import { Page } from 'puppeteer';
import BaseMenu from './base-menu';
import Checkbox from 'app/element/checkbox';
import { getPropValue } from 'utils/element-utils'
import AdminTable from './admin-table';

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

  //get status of each bypass modules individually
  async getAccessModuleStatus(accessModule: string): Promise<boolean> {
    const selector = this.getMenuItemXpath(accessModule);
    const btn = new Checkbox(this.page, selector);
    const accessModuleStatus =await getPropValue<boolean>(await btn.asElementHandle(), 'checked');
    return accessModuleStatus;
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

  async getBypassModuleTest(): Promise<void> {
    await this.getAccessModuleStatus('Two Factor Auth');
    await this.getAccessModuleStatus('Compliance Training');
    await this.getAccessModuleStatus('Two Factor Auth');
    await this.getAccessModuleStatus('Data User Code of Conduct');
    await this.getAccessModuleStatus('RAS Login.gov Link');
  }
 
  async getBypassModuleStaging(): Promise<void> {
    await this.getAccessModuleStatus('Two Factor Auth');
    await this.getAccessModuleStatus('Two Factor Auth');
    await this.getAccessModuleStatus('Data User Code of Conduct');
  }

  async getBypassModuleStatus(): Promise<void>{
    //const userAdminPage = new UserAdminPage(page);
    const userAdminPage = new AdminTable(page);
    //const dataTable = userAdminPage.getUserAdminTable();
    const userTableTest = await userAdminPage.getColumnNames();
    let testTable = ['Training', 'RAS Login.gov Link'];
    if (testTable.some(i => userTableTest.includes(i))){
      return this.getBypassModuleTest();
    }else{
      return this.getBypassModuleStaging();
    }
  }
}
