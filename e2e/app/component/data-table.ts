import { Page } from 'puppeteer';
import Container from 'app/container';
import { getPropValue } from 'utils/element-utils';
import Table from './table';
import Textbox from '../element/textbox';

const defaultXpath = '//*[contains(concat(" ", normalize-space(@class), " "), " p-datatable ")]';

export default class DataTable extends Table {
  constructor(page: Page, opts: { xpath?: string; container?: Container } = {}) {
    const { xpath = defaultXpath, container } = opts;
    super(page, xpath, container);
  }

  getHeaderTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}//table[@class="p-datatable-scrollable-header-table" or @role="grid"]`
    );
  }

  getTable(): Table {
    return new Table(
      this.page,
      `${this.getXpath()}//table[contains(concat(" ", normalize-space(@class), " "), " p-datatable-table ")]`
    );
  }

  getFooterTable(): Table {
    return new Table(this.page, `${this.getXpath()}//table[@class="p-datatable-scrollable-footer-table"]`);
  }

  async getNumRecords(): Promise<number[]> {
    const selector = `${this.getPaginatorXpath()}/*[@class="p-paginator-current"]`;
    let textContent: string;
    try {
      const element = await this.page.waitForXPath(selector, { visible: true });
      textContent = await getPropValue<string>(element, 'textContent');
    } catch (e) {
      return [0, 0, 0];
    }

    // parse for total records. expected string format is "76 - 100 of 100 records".
    const [start, end, total] = textContent.match(/\d+/g);
    return [Number(start), Number(end), Number(total)];
  }

  private getPaginatorXpath(): string {
    return `${this.getXpath()}/*[contains(concat(normalize-space(@class), " "), "p-paginator ")]`;
  }

  public getSearchNameBox(): Textbox {
    return Textbox.findByName(this.page, { name: 'Search Name' });
  }
}
