import {ElementHandle, Page} from 'puppeteer';
import Container from 'app/container';

// Table column names
export enum TableColumn {
   // Cohorts Criteria dialog: Condition search results table column names
   Name = 'Name',
   Code = 'Code',
   Vocab = 'Vocab',
   Count = 'Count',
   ViewHierarchy =  'View Hierarchy',
   // Add here some other table column names ...
}

export default class Table extends Container {

  private trXpath: string = this.xpath + '/tbody/tr';

  constructor(page: Page, xpath: string, container?: Container) {
    super(page, (container === undefined) ? xpath : `${container.getXpath()}${xpath}`);
  }

  async exists(): Promise<boolean> {
    try {
      await this.page.waitForXPath(this.xpath, {timeout: 1000});
      return true;
    } catch (e) {
      return false;
    }
  }

  async findCell(rowIndex: number, columnIndex: number): Promise<ElementHandle> {
    const cellXpath = this.getCellXpath(rowIndex, columnIndex);
    return this.page.waitForXPath(cellXpath);
  }

  async findCellValue(rowIndex: number, columnIndex: number): Promise<string> {
    const handle = await this.findCell(rowIndex, columnIndex);
    const prop = await handle.getProperty('innerText')
    return (await prop.jsonValue()).toString();
  }

   /**
    * Returns a table row.
    * @param {number} rowNum Row number starts at 1.
    */
  async getRow(rowNum: number): Promise<ElementHandle> {
      // $x() returns zero-indexed array
    const rows = await this.page.$x(this.trXpath);
    if (rows.length < rowNum) {
      throw new Error(`Table row ${rowNum} not found.`);
    }
    return rows[rowNum - 1];
  }

  async getRowCount(): Promise<number> {
    return (await this.page.$x(this.trXpath)).length;
  }

  async getColumnIndex(columName: string): Promise<void> {
    const columnIndexXpath = `count(${this.xpath}/thead/tr/th[contains(normalize-space(text()), "${columName}")]/preceding-sibling::*)`;
    const handle = await this.page.waitForXPath(columnIndexXpath, {visible: true});
    console.log('getColumnIndex: ' + await handle.jsonValue());
  }

  getCellXpath(rowIndex: number, columnIndex: number): string {
    return `${this.trXpath}[${rowIndex}]/td[${columnIndex}]`;
  }

}
