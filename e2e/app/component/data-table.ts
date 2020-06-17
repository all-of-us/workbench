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


}
