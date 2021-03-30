import { ElementHandle, Page } from 'puppeteer';
import Container from 'app/container';
import { getPropValue } from 'utils/element-utils';

// Table column names
export enum TableColumn {
  // Cohorts Criteria dialog: Condition search results table column names
  Name = 'Name',
  Code = 'Code',
  Vocab = 'Vocab',
  Count = 'Count',
  ViewHierarchy = 'View Hierarchy'
  // Add here some other table column names ...
}

export default class Table extends Container {
  private trXpath: string = this.xpath + '//tbody/tr';
  private theadXpath: string = this.xpath + '/thead/tr/th';

  constructor(page: Page, xpath: string, container?: Container) {
    super(page, container === undefined ? xpath : `${container.getXpath()}${xpath}`);
  }

  async asElement(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, { timeout: 1000, visible: true }).then((elemt) => elemt.asElement());
  }

  async exists(): Promise<boolean> {
    try {
      return (await this.asElement()) !== null;
    } catch (e) {
      return false;
    }
  }

  async waitForVisible(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, { visible: true });
  }

  async getCell(rowIndex: number, columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getCellXpath(rowIndex, columnIndex);
    return this.page.waitForXPath(cellXpath, { visible: true });
  }

  async getCellValue(rowIndex: number, columnIndex: number): Promise<string> {
    const handle = await this.getCell(rowIndex, columnIndex);
    return getPropValue<string>(handle, 'innerText');
  }

  async getRows(): Promise<ElementHandle[]> {
    return this.page.$x(this.trXpath);
  }

  async getColumns(): Promise<ElementHandle[]> {
    return this.page.$x(this.theadXpath);
  }

  /**
   * Returns a table row.
   * @param {number} rowNum Row number starts at 1.
   */
  async getRow(rowNum: number): Promise<ElementHandle> {
    // $x() returns zero-indexed array
    const rows = await this.getRows();
    if (rows.length < rowNum) {
      throw new Error(`Table row ${rowNum} not found.`);
    }
    return rows[rowNum - 1];
  }

  async getRowCount(): Promise<number> {
    return (await this.getRows()).length;
  }

  async getColumnCount(): Promise<number> {
    return (await this.getColumns()).length;
  }

  /**
   * Finds table column names. Returns in array of string.
   * @returns {Array<string>}
   */
  async getColumnNames(): Promise<string[]> {
    const columns = await this.getColumns();
    const columnNames = [];
    for (const column of columns) {
      const textContent = await getPropValue<string>(column, 'innerText');
      columnNames.push(textContent);
      await column.dispose();
    }
    return columnNames;
  }

  async getColumnIndex(columName: string): Promise<number> {
    const columnIndexXpath = `count(${this.theadXpath}[contains(normalize-space(text()), "${columName}")]/preceding-sibling::*)`;
    const handle = await this.page.waitForXPath(columnIndexXpath, { visible: true });
    const value = await handle.jsonValue();
    console.log('getColumnIndex: ' + value);
    return Number(value.toString());
  }

  async getHeaderCell(columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getHeaderXpath(columnIndex);
    return this.page.waitForXPath(cellXpath, { visible: true });
  }

  getCellXpath(rowIndex: number, columnIndex: number): string {
    return `${this.trXpath}[${rowIndex}]/td[${columnIndex}]`;
  }

  getHeaderXpath(columnIndex: number): string {
    return `${this.theadXpath}[${columnIndex}]`;
  }
}
