import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import Textbox from 'app/element/textbox';
import DataTable from 'app/component/data-table';
import Button from 'app/element/button';
import AuthenticatedPage from './authenticated-page';
import ConceptsetSaveModal, {SaveOption} from './conceptset-save-modal';

const PageTitle = 'Search Concepts';

export default class ConceptsetSearchPage extends AuthenticatedPage{

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    const searchTextbox = this.getSearchTextbox();
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        searchTextbox.waitForXPath(),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.error(`ConceptsetSearchPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async saveConcept(saveOption?: SaveOption, existingConceptName?: string): Promise<string> {
    const modal = new ConceptsetSaveModal(this.page);
    return modal.fillOutSaveModal(saveOption, existingConceptName);
  }

  async getAddToSetButton(): Promise<Button> {
    return new Button(this.page, '//*[@data-test-id="sliding-button"]');
  }

  async clickAddToSetButton(): Promise<string> {
    const addButton = await this.getAddToSetButton();
    const textContent = addButton.getTextContent();
    await addButton.click();
    await waitWhileLoading(this.page);
    return textContent;
  }

  async searchConcepts(searchKeywords: string): Promise<void> {
    const searchInput = this.getSearchTextbox();
    await searchInput.type(searchKeywords);
    await searchInput.pressReturn();
    return waitWhileLoading(this.page);
  }

  /**
   * Check Checkbox in specified row index
   * @param {number} rowIndex Row index.
   * @param {number} selctionColumnIndex Selection column index.
   * @return {code: string; vocabulary: string; participantCount: string}
   *  The Code, Vocabulary and Participant Count values in same table row.
   */
  async dataTableSelectRow(rowIndex: number = 1,
                           selctionColumnIndex = 1): Promise<{name: string, code: string; vocabulary: string; participantCount: string}> {
    const dataTable = this.getDataTable();
    const bodyTable = dataTable.getBodyTable();

    // Name column #2
    const nameCell = await bodyTable.getCell(rowIndex, 2);
    let textProp = await nameCell.getProperty('textContent');
    const nameValue = (await textProp.jsonValue()).toString();

    // Code column #3
    const codeCell = await bodyTable.getCell(rowIndex, 3);
    textProp = await codeCell.getProperty('textContent');
    const codeValue = (await textProp.jsonValue()).toString();

    // Vocabulary column #4
    const vocabularyCell = await bodyTable.getCell(rowIndex, 4);
    textProp = await vocabularyCell.getProperty('textContent');
    const vocabValue = (await textProp.jsonValue()).toString();

    // Participant Count column #5
    const participantCountCell = await bodyTable.getCell(rowIndex, 5);
    textProp = await participantCountCell.getProperty('textContent');
    const partiCountValue = (await textProp.jsonValue()).toString();

    const selectCheckCell = await bodyTable.getCell(rowIndex, selctionColumnIndex);
    const elemt = (await selectCheckCell.$x('.//*[@role="checkbox"]'))[0];
    await elemt.click();

    return { name: nameValue, code: codeValue, vocabulary: vocabValue, participantCount: partiCountValue };
  }

  /**
   * Check Select All Checkbox in data table.
   */
  async dataTableSelectAllRows(): Promise<void> {
    const dataTable = this.getDataTable();
    const headerTable = dataTable.getHeaderTable();

    const selectCheckCell = await headerTable.getHeaderCell(1);
    const elemt = (await selectCheckCell.$x('.//*[@role="checkbox"]'))[0];
    await elemt.click();
  }

  getDataTable(): DataTable {
    return new DataTable(this.page);
  }

  private getSearchTextbox(): Textbox {
    return new Textbox(this.page, '//input[@data-test-id="concept-search-input"]');
  }

}
