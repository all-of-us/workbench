import { Page } from 'puppeteer';
import Table from 'app/component/table';
import Textbox from 'app/element/textbox';
import { waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';

export default class CriteriaSearchPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * @deprecated
   */
  async isLoaded(): Promise<boolean> {
    await Promise.all([this.page.waitForXPath('//*[@id="criteria-search-container"]', { visible: true })]);
    await waitWhileLoading(this.page);
    return true;
  }

  /**
   * @deprecated
   */
  getConditionSearchResultsTable(): Table {
    return new Table(this.page, '//table[@class="p-datatable"]');
  }

  /**
   * @deprecated
   */
  async searchCriteria(searchWord: string): Promise<Table> {
    const resultsTable = new Table(this.page, '//table[@class="p-datatable"]');
    const searchFilterTextbox = Textbox.findByName(this.page, { containsText: 'by code or description' });
    await searchFilterTextbox.type(searchWord);
    await searchFilterTextbox.pressReturn();
    await waitWhileLoading(this.page);
    return resultsTable;
  }

  /**
   * @deprecated
   */
  getResultsTable(): Table {
    return new Table(this.page, '//table[@data-test-id="list-search-results-table"]');
  }

  /**
   * @deprecated
   */
  async resultsTableSelectRow(
    rowIndex = 1,
    selectionColumnIndex = 1
  ): Promise<{ name: string; code: string; vocabulary: string; rollUpCount: string }> {
    const resultsTable = this.getResultsTable();

    // Name column #1
    const nameCell = await resultsTable.getCell(rowIndex, 1);
    const nameElem = (await nameCell.$x('.//div[@data-test-id="name-column-value"]'))[0];
    const nameValue = await getPropValue<string>(nameElem, 'textContent');

    // Code column #2
    const codeCell = await resultsTable.getCell(rowIndex, 2);
    const codeValue = await getPropValue<string>(codeCell, 'textContent');

    // Vocabulary column #3
    const vocabularyCell = await resultsTable.getCell(rowIndex, 3);
    const vocabValue = await getPropValue<string>(vocabularyCell, 'textContent');

    // Roll-up Count column #6
    const rollUpCountCell = await resultsTable.getCell(rowIndex, 6);
    const rollUpCountValue = await getPropValue<string>(rollUpCountCell, 'textContent');

    const selectCheckCell = await resultsTable.getCell(rowIndex, selectionColumnIndex);
    const elemt = (await selectCheckCell.$x('.//*[@shape="plus-circle"]'))[0];
    await elemt.click();

    return { name: nameValue, code: codeValue, vocabulary: vocabValue, rollUpCount: rollUpCountValue };
  }
}
