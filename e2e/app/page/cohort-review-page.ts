import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import DataTable from 'app/component/data-table';
import Button from 'app/element/button';
import {LinkText} from 'app/page-identifiers';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'Review Cohort Participants';

export default class CohortReviewPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
        this.getDataTable().exists(),
      ]);
      return true;
    } catch (e) {
      console.log(`CohortReviewPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  getDataTable(): DataTable {
    return new DataTable(this.page);
  }

  async getBackToCohortButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.BackToCohort});
  }

   /**
    * Click participant link in specified row
    * @param {number} rowIndex
    * @return {string} Participant text that was clicked on.
    */
  async clickParticipantLink(rowIndex: number = 1, colIndex: number = 1): Promise<string> {
    const dataTable = this.getDataTable();
    const bodyTable = dataTable.getBodyTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textProp = await cell.getProperty('textContent');
    const cellTextContent = await textProp.jsonValue();
    await cell.click();
    await waitWhileLoading(this.page);
    return cellTextContent.toString();
  }

}
