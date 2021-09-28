import Container from 'app/container';
import { ElementHandle, Page } from 'puppeteer';
import Table from './table';

const defaultXpath = '//*[contains(concat(normalize-space(@class), " "), "p-datatable ")]';

export default class adminTable extends Table {
  protected userLinkElement: ElementHandle;
  columns1: any;

  constructor(page: Page, xpath: string = defaultXpath, container?: Container) {
    super(page, xpath, container);
  }

  asElementHandle(): ElementHandle {
    return this.userLinkElement.asElement();
  }

  getFrozenHeader(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-scrollable-view p-datatable-frozen-view"]
      //table[@class="p-datatable-scrollable-header-table"]`
    );
  }

  getFrozenBody(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-scrollable-view p-datatable-frozen-view"]
      //table[@class="p-datatable-scrollable-body-table"]`
    );
  }

  getHeaderTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]
      //table[@class="p-datatable-scrollable-header-table"]`
    );
  }

  getBodyTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]
      //table[@class="p-datatable-scrollable-body-table"]`
    );
  }

  getFooterTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-scrollable-view p-datatable-unfrozen-view"]
      //table[@class="p-datatable-scrollable-footer-table"]`
    );
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
  //  async getAllColumnNames(): Promise<string[]> {
  //   const colnames = await this.getUnfrozenColNames();
  //   return colnames;
  // }

  async getAllColumnNames(): Promise<string[]> {
    const columns1: Array<string> = await this.getFrozenColNames();
    console.log(`columns1: ${columns1}`);
    const columns2: Array<string> = await this.getUnfrozenColNames();
    console.log(`columns2: ${columns2}`);
    const allColumnNames = columns1.concat(columns2);
    console.log(`allColumnNames: ${allColumnNames}`);
    return allColumnNames;
  }

  getUnfrozenColNames(): Promise<string[]> {
    const headerTable = this.getHeaderTable();
    return headerTable.getColumnNames();
  }

  getFrozenColNames(): Promise<string[]> {
    const headerTable = this.getFrozenHeader();
    return headerTable.getColumnNames();
  }

  async getNameColindex(): Promise<number> {
    // const dataTable = this.getFrozenHeader();
    // const columnName = await dataTable.getColumnNames();
    const columnName = await this.getFrozenColNames();
    const colIndexNum = columnName.indexOf('Name');
    return colIndexNum + 1;
  }
}
