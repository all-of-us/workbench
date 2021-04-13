import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Textbox from 'app/element/textbox';
import DataTable from 'app/component/data-table';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import { getPropValue, waitUntilChanged } from 'utils/element-utils';
import ReviewConceptSetSidebar from 'app/component/review-conceptset-sidebar';
import AuthenticatedPage from './authenticated-page';
import ConceptSetSaveModal, { SaveOption } from 'app/modal/conceptset-save-modal';

const PageTitle = 'Search Concepts';

export default class ConceptSetSearchPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    const searchTextbox = this.getSearchTextbox();
    await searchTextbox.waitForXPath();
    return true;
  }

  async saveConceptSet(saveOption?: SaveOption, existingConceptSetName?: string): Promise<string> {
    const modal = new ConceptSetSaveModal(this.page);
    await modal.waitForLoad();
    return modal.fillOutSaveModal(saveOption, existingConceptSetName);
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
    const dataTable = await this.getDataTable().asElement();
    const searchInput = this.getSearchTextbox();
    await searchInput.type(searchKeywords);
    await searchInput.pressReturn();
    await waitUntilChanged(this.page, dataTable);
    return waitWhileLoading(this.page);
  }

  /**
   * Check Checkbox in specified row index
   * @param {number} rowIndex Row index.
   * @param {number} selctionColumnIndex Selection column index.
   * @return {code: string; vocabulary: string; participantCount: string}
   *  The Code, Vocabulary and Participant Count values in same table row.
   */
  async dataTableSelectRow(
    rowIndex: number = 1,
    selctionColumnIndex = 1
  ): Promise<{ name: string; code: string; vocabulary: string; participantCount: string }> {
    const dataTable = this.getDataTable();
    const bodyTable = dataTable.getBodyTable();

    // Name column #2
    const nameCell = await bodyTable.getCell(rowIndex, 2);
    const nameValue = await getPropValue<string>(nameCell, 'textContent');

    // Code column #3
    const codeCell = await bodyTable.getCell(rowIndex, 3);
    const codeValue = await getPropValue<string>(codeCell, 'textContent');

    // Vocabulary column #4
    const vocabularyCell = await bodyTable.getCell(rowIndex, 4);
    const vocabValue = await getPropValue<string>(vocabularyCell, 'textContent');

    // Participant Count column #5
    const participantCountCell = await bodyTable.getCell(rowIndex, 5);
    const partiCountValue = await getPropValue<string>(participantCountCell, 'textContent');

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

  async reviewAndSaveConceptSet(): Promise<void> {
    const finishAndReviewButton = await Button.findByName(this.page, { name: LinkText.FinishAndReview });
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Click Save Concept Set button in sidebar
    const reviewConceptSetSidebar = new ReviewConceptSetSidebar(this.page);
    await reviewConceptSetSidebar.waitUntilVisible();
    await reviewConceptSetSidebar.clickSaveConceptSetButton();
  }
}
