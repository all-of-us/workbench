import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import Table from 'app/component/table';
import AdminTable from 'app/component/admin-table';
import Textbox from 'app/element/textbox';
import BypassPopup from 'app/component/bypass-modules';
import UserAuditPage from './admin-user-audit-page';
import UserProfileAdminPage from './user-profile-admin-page';

const PageTitle = 'User Admin Table';

export default class UserAdminTablePage extends AuthenticatedPage {
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
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    await getPropValue<string>(cell, 'textContent');
    await cell.click();
    const bypassPopup = new BypassPopup(this.page);
    return bypassPopup;
  }

  // click on the name link in the frozen column to navigate to the admin user profile page
  async clickNameLink(rowIndex = 1, colIndex = 1): Promise<UserProfileAdminPage> {
    const dataTable = this.getUserAdminTable();
    const cell = await dataTable.getCellLink(rowIndex, colIndex);
    await cell.click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === this.page.target());
    const newPage = await newTarget.page();
    return new UserProfileAdminPage(newPage).waitForLoad();
  }

  async clickUserName(rowIndex = 1, colIndex = 1): Promise<void> {
    const dataTable = this.getUserAdminTable();
    const cell = await dataTable.getCellLink(rowIndex, colIndex);
    await cell.click();
  }

  // get the username email
  async getUserNameEmail(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  //extract only the Username text to verify the search box result
  async getUsernameText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    return getPropValue<string>(cell, 'innerText');
  }

  // extract only the User Lockout text
  async getUserLockoutText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  // extract only the status text to verify the user status
  async getStatusText(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }

  // click on the Audit link to navigate to the user audit page
  async clickAuditLink(rowIndex = 1, colIndex = 1): Promise<UserAuditPage> {
    const dataTable = this.getUserAdminTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCellLink(rowIndex, colIndex);
    await getPropValue<string>(cell, 'textContent');
    await cell.click();
    const newTarget = await browser.waitForTarget((target) => target.opener() === this.page.target());
    const newPage = await newTarget.page();
    await waitWhileLoading(this.page);
    return new UserAuditPage(newPage).waitForLoad();
  }
}
