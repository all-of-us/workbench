import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import Table from 'app/component/table';
import AdminTable from 'app/component/admin-table';
import Textbox from 'app/element/textbox';
import BypassPopup from 'app/component/bypass-modules';
import UserAuditPage from './admin-user-audit-page';
import UserProfileInfo from './admin-user-profile-info';

const PageTitle = 'User Admin Table';

export default class UserAdminPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    await this.getUserAdminTable().exists();
    return true;
  }

  getUserAdminTable(): AdminTable {
    return new AdminTable(this.page);
  }

  waitForSearchBox(): Textbox {
    return Textbox.findByName(this.page, { name: 'Search' });
  }

  async searchUser(username: string): Promise<Table> {
    const userAdminTable = this.getUserAdminTable();
    const searchBox = this.waitForSearchBox();
    await searchBox.type(username);
    await waitWhileLoading(this.page);
    return userAdminTable;
  }

  /**
   * username rowindex & bypasslink colindex.
   * @param {number} colIndex column index
   * @param {number} rowIndex row index
   */

  // click on the Bypass link to access the bypass modal
  async clickBypassLink(rowIndex = 1, colIndex = 1): Promise<BypassPopup> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    await getPropValue<string>(cell, 'textContent');
    await cell.click();
    const bypassPopup = new BypassPopup(this.page);
    return bypassPopup;
  }

  // click on the name link in the frozen column to navigate to the user info page
  async clickNameLink(rowIndex = 1, colIndex = 1): Promise<UserProfileInfo> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getFrozenBody();
    const cell = await bodyTable.getCellLink(rowIndex, colIndex);
    console.log(`cell: ${cell}`);
    const textContent = await getPropValue<string>(cell, 'textContent');
    console.log(`textContent : ${textContent}`);
    await cell.click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target()); 
    const newPage = await newTarget.page();
    return new UserProfileInfo(newPage).waitForLoad();
  }

 //extract only the User name text to verify the search box result
  async getUserNameText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  // extract only the User Lockout text 
  async getUserLockoutText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  // extract only the status text to verify the user status
  async getStatusText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  // click on the Audit link to navigate to the user audit page
  async clickAuditLink(rowIndex = 1, colIndex = 1): Promise<UserAuditPage> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCellLink(rowIndex, colIndex);
    await getPropValue<string>(cell, 'textContent');
    await cell.click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === page.target()); 
    const newPage = await newTarget.page();
    return new UserAuditPage(newPage).waitForLoad();
  }
}
