import Container from 'app/container';
import { ElementHandle, Page } from 'puppeteer';
import Table from './table';

const defaultXpath = '//*[contains(concat(normalize-space(@class), " "), "p-datatable ")]';

export default class adminTable extends Table {
  protected userLinkElement: ElementHandle;

  constructor(page: Page, xpath: string = defaultXpath, container?: Container) {
    super(page, xpath, container);
  }

  asElementHandle(): ElementHandle {
    return this.userLinkElement.asElement();
  }

  getHeaderRow(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-wrapper"]
      //table[@class="p-datatable-table"]
      //thead//tr`
    );
  }

  getTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}
      //div[@class="p-datatable-wrapper"]
      //table[@class="p-datatable-table"]`
    );
  }

  // gets the column index
  async getColumnIndex(columnName: string): Promise<number> {
    const columnNames = await this.getColumnNames();
    const colIndexNum = columnNames.indexOf(columnName);
    return colIndexNum + 1;
  }

  /**
   * Finds table column names. Returns in array of string.
   * @returns {Array<string>}
   */
  async getColumnNames(): Promise<string[]> {
    const table = this.getTable();
    return table.getColumnNames();
  }

  async getNameColIndex(): Promise<number> {
    const columnNames = await this.getColumnNames();
    const colIndexNum = columnNames.indexOf('Name');
    return colIndexNum + 1;
  }
}
