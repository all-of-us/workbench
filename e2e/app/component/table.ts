import { ElementHandle, Page } from 'puppeteer';
import Container from 'app/container';
import { getPropValue } from 'utils/element-utils';
import Cell from './cell';

export default class Table extends Container {
  constructor(page: Page, xpath: string, container?: Container) {
    super(page, container === undefined ? xpath : `${container.getXpath()}${xpath}`);
  }

  async exists(): Promise<boolean> {
    try {
      return (await this.asElement()) !== null;
    } catch (e) {
      return false;
    }
  }

  async getCell(rowIndex: number, columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getCellXpath(rowIndex, columnIndex);
    return this.page.waitForXPath(cellXpath, { visible: true });
  }

  async getCellLink(rowIndex: number, columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getCellXpath(rowIndex, columnIndex) + '/a';
    return this.page.waitForXPath(cellXpath, { visible: true });
  }

  async getCellValue(rowIndex: number, columnIndex: number): Promise<string> {
    const handle = await this.getCell(rowIndex, columnIndex);
    return getPropValue<string>(handle, 'innerText');
  }

  async getRows(): Promise<ElementHandle[]> {
    return this.page.$x(this.getTrXpath());
  }

  async getColumns(): Promise<ElementHandle[]> {
    return this.page.$x(this.getTheadXpath());
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
    const columnNames: string[] = [];
    for (const column of columns) {
      const textContent = await getPropValue<string>(column, 'innerText');
      columnNames.push(textContent);
      await column.dispose();
    }
    return columnNames;
  }

  async getRowValues(columnIndex = 1): Promise<string[]> {
    const rows = await this.getRows();
    const rowValues: string[] = [];
    for (let i = 1; i <= rows.length; i++) {
      const cell = await this.getCell(i, columnIndex);
      const textContent = await getPropValue<string>(cell, 'innerText');
      rowValues.push(textContent);
    }
    return rowValues;
  }

  async getColumnIndex(columnName: string): Promise<number> {
    const indexXpath =
      `count(${this.getTheadXpath()}[contains(normalize-space(text()), "${columnName}")]` + '/preceding-sibling::*)';
    const handle = await this.page.waitForXPath(indexXpath, { visible: true });
    const value = await handle.jsonValue();
    return Number(value.toString());
  }

  async getHeaderCell(columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getHeaderXpath(columnIndex);
    return this.page.waitForXPath(cellXpath, { visible: true });
  }

  /**
   * In table, find the cell value in one column based on the cell value in another column.
   * Two table cells have to be in the same row.
   *
   * @param searchColumnHeader Column header: Search for cell value in this column. The row that contains this cell is
   *   used to find the other cell in resultColumnHeader
   * @param searchCellValue Cell value: Text to search for in searchColumnHeader
   * @param resultColumnHeader Column header: Find the cell in this column in the same row
   *
   * @returns Table cell in resultColumnHeader
   */
  async findCellByRowValue(
    searchColumnHeader: string,
    searchCellValue: string,
    resultColumnHeader: string
  ): Promise<Cell | null> {
    // Find the searchColumnHeader index
    const allColumns = await this.getColumns();
    const colValues = await Promise.all(allColumns.map((column) => getPropValue<string>(column, 'innerText')));

    const columnIndex = colValues.findIndex((v) => v === searchColumnHeader);
    const resultColumnIndex = colValues.findIndex((v) => v === resultColumnHeader);
    if (columnIndex === -1 || resultColumnIndex === -1) {
      return null;
    }

    // Find the row index which contains searchCellValue
    const allRows = await this.getRows();
    const searchColumnTdValues = await Promise.all(
      allRows.map(async (row) => {
        const td = await row.$x('./td');
        return getPropValue<string>(td[columnIndex], 'innerText');
      })
    );

    const searchRowIndex = searchColumnTdValues.findIndex((v) => v === searchCellValue);
    if (searchRowIndex === -1) {
      return null;
    }

    return new Cell(this.page, this.getCellXpath(searchRowIndex + 1, resultColumnIndex + 1));
  }

  getCellXpath(rowIndex: number, columnIndex: number): string {
    return `${this.getTrXpath()}[${rowIndex}]/td[${columnIndex}]`;
  }

  getHeaderXpath(columnIndex: number): string {
    return `${this.getTheadXpath()}[${columnIndex}]`;
  }

  private getTrXpath(): string {
    return this.getXpath() + '//tbody/tr';
  }

  private getTheadXpath(): string {
    return this.getXpath() + '//thead/tr/th';
  }
}
