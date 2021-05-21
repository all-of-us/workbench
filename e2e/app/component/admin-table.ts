import { Page } from 'puppeteer';
import Container from 'app/container';
//import { getPropValue } from 'utils/element-utils';
import Table from './table';


const defaultXpath = '//*[contains(concat(normalize-space(@class), " "), "p-datatable ")]';

export default class adminTable extends Table {
  constructor(page: Page, xpath: string = defaultXpath, container?: Container) {
    super(page, xpath, container);
  }

  getFrozenHeader(): Table {
    return new Table(this.page, `${this.getXpath()}//div[@class="p-datatable-scrollable-view p-datatable-frozen-view"]//table[@class="p-datatable-scrollable-header-table"]`);
  }

  getFrozenBody(): Table {
    return new Table(this.page, `${this.getXpath()}//div[@class="p-datatable-scrollable-view p-datatable-frozen-view"]//table[@class="p-datatable-scrollable-body-table"]`);
  }

  getHeaderTable(): Table {
    return new Table(this.page, `${this.getXpath()}//div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]//table[@class="p-datatable-scrollable-header-table"]`);
  }

  getBodyTable(): Table {
    return new Table(this.page, `${this.getXpath()}//div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]//table[@class="p-datatable-scrollable-body-table"]`);
  }

  getFooterTable(): Table {
    return new Table(this.page, `${this.getXpath()}//div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]//table[@class="p-datatable-scrollable-footer-table"]`);
  }

  // gets the column index
  async getColumnIndex(header: string): Promise<number> {
    const headerTable = this.getHeaderTable();
    const columnNames = await headerTable.getColumnNames();
    const colIndexNum = columnNames.indexOf(header);
    return colIndexNum + 1;
  }

  /**
   * Finds table column names. Returns in array of string.
   * @returns {Array<string>}
   */
  async getColumnNames(): Promise<string[]> {
    const headerTable = this.getHeaderTable();
    return headerTable.getColumnNames();
  }

  async getNameColindex(): Promise<number> {
    const dataTable = this.getFrozenHeader();
    const columnName = await dataTable.getColumnNames();
    const colIndexNum = columnName.indexOf('Name');
    console.log(columnName);
    return colIndexNum + 1;
  }
}
