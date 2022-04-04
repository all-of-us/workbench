import BaseSidebar from './base-sidebar';
import { SideBarLink } from 'app/text-labels';
import { logger } from 'libs/logger';
import { Page } from 'puppeteer';
import DataTable from 'app/component/data-table';
import { waitWhileLoading } from 'utils/waits-utils';
import { exists } from 'utils/element-utils';

export default class GenomicExtractionsSidebar extends BaseSidebar {
  constructor(page: Page) {
    super(page);
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) {
      return;
    }
    await this.clickIcon(SideBarLink.GenomicExtractionsHistory);
    await this.waitUntilVisible();
    // Wait for visible text
    await this.page.waitForXPath(`${this.getXpath()}//h3[normalize-space(text())="Genomic Extractions"]`, {
      visible: true
    });
    // Wait for visible button
    await this.page.waitForXPath(`${this.getXpath()}//*[@role="button"]`, { visible: true });
    logger.info(`Opened "${await this.getTitle()}" Genomic Extractions History sidebar`);
  }

  getHistoryTable(): DataTable {
    return new DataTable(this.page, { container: this });
  }

  // Extraction spinner status.
  async isInProgress(datasetName: string, timeout?: number): Promise<boolean> {
    const statusSpinnerXpath = await this.getStatusSpinnerXpath(datasetName);
    // Look for the spinner in the table. Return true when it's found. Otherwise return false.
    return this.page
      .waitForXPath(statusSpinnerXpath, {
        timeout: timeout
      })
      .then(() => true)
      .catch(() => false);
  }

  /**
   * Open sidebar, wait for finished status, close sidebar.
   */
  async waitForJobDone(datasetName: string, timeout?: number): Promise<boolean> {
    await this.open();
    const inProgress = await this.isInProgress(datasetName, timeout);
    await this.close();
    return !inProgress;
  }

  /**
   * Look for job's success icon in Genomic Extraction History table in Extraction sidebar.
   */
  async isJobSuccess(datasetName: string): Promise<boolean> {
    await this.open();
    const statusSuccessXpath = await this.getStatusSuccessXpath(datasetName);
    return await this.page
      .waitForXPath(statusSuccessXpath, {
        visible: true,
        timeout: 5000
      })
      .then(() => true)
      .catch(() => false);
  }

  private async getStatusSpinnerXpath(datasetName: string): Promise<string> {
    const historyTable = this.getHistoryTable();
    await historyTable.waitUntilVisible();
    const statusCell = await historyTable.findCellByRowValue('DATASET NAME', datasetName, 'STATUS');
    return (
      statusCell.getXpath() +
      '/*[.//*[@data-icon="sync-alt" and @role="img" and contains(@style,"animation-name: spin")]]'
    );
  }

  private async getStatusSuccessXpath(datasetName: string): Promise<string> {
    const historyTable = this.getHistoryTable();
    await historyTable.waitUntilVisible();
    const statusCell = await historyTable.findCellByRowValue('DATASET NAME', datasetName, 'STATUS');
    return statusCell.getXpath() + '/*[.//*[@data-icon="check-circle" and @role="img"]]';
  }
}
