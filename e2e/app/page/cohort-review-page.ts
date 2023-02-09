import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import DataTable from 'app/component/data-table';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import { LinkText } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'Review Cohort Participants';

export default class CohortReviewPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await this.getDataTable().exists();
    await waitWhileLoading(this.page);
    return true;
  }

  getDataTable(): DataTable {
    return new DataTable(this.page);
  }

  getCreateCohortReviewIcon(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { iconShape: 'plus-circle' });
  }

  getBackToCohortButton(): Button {
    return Button.findByName(this.page, { name: LinkText.BackToCohort });
  }

  /**
   * Click participant link in specified row
   * @param {number} rowIndex
   * @return {string} Participant text that was clicked on.
   */
  async clickParticipantLink(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getDataTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const innerText = await getPropValue<string>(cell, 'innerText');
    await cell.click();
    await waitWhileLoading(this.page);
    return innerText;
  }
  /**
   * Get the participant ID in specified row
   * @param {number} rowIndex
   * @return {string} Participant text that was clicked on.
   */
  async getParticipantLinkId(rowIndex = 1, colIndex = 1): Promise<string> {
    const dataTable = this.getDataTable();
    const bodyTable = dataTable.getTable();
    const cell = await bodyTable.getCell(rowIndex, colIndex);
    const textContent = await getPropValue<string>(cell, 'textContent');
    return textContent;
  }
}
