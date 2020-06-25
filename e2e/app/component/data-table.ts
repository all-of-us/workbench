import {Page} from 'puppeteer';
import Container from 'app/container';
import Table from './table';


const defaultXpath = '//*[contains(concat(normalize-space(@class), " "), "p-datatable ")]';

export default class DataTable extends Table {

  constructor(page: Page, xpath: string = defaultXpath, container?: Container) {
    super(page, xpath, container);
  }

  getHeaderTable(): Table {
    return new Table(this.page, `${this.getXpath()}//table[@class="p-datatable-scrollable-header-table"]`)
  }

  getBodyTable(): Table {
    return new Table(this.page, `${this.getXpath()}//table[@class="p-datatable-scrollable-body-table"]`)
  }

  getFooterTable(): Table {
    return new Table(this.page, `${this.getXpath()}//table[@class="p-datatable-scrollable-footer-table"]`)
  }

  /**
   * Finds table column names. Returns in array of string.
   * @returns {Array<string>}
   */
  async getColumnNames(): Promise<string[]> {
    const headerTable = this.getHeaderTable();
    return headerTable.getColumnNames();
  }

  async getNumRecords(): Promise<number[]> {
    const selector = `${this.getPaginatorXpath()}/*[@class="p-paginator-current"]`;
    let value;
    try {
      const elemt = await this.page.waitForXPath(selector);
      const textContent = await elemt.getProperty('textContent');
      value = await textContent.jsonValue();
    } catch (e) {
      return [0, 0, 0];
    }

    // parse for total records. expected string format is "76 - 100 of 100 records".
    const [start, end, total] = value.toString().match(/\d+/g);
    return [start, end, total];
  }

  private getPaginatorXpath(): string {
    return `${this.getXpath()}/*[contains(concat(normalize-space(@class), " "), "p-paginator ")]`
  }


}
