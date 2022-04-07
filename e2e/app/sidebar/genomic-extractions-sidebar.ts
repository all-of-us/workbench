import BaseSidebar from './base-sidebar';
import { SideBarLink } from 'app/text-labels';
import { logger } from 'libs/logger';
import { Page } from 'puppeteer';
import DataTable from 'app/component/data-table';
import { waitWhileLoading } from 'utils/waits-utils';
import { elementExists } from 'utils/element-utils';

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
  async isInProgress(timeout?: number): Promise<boolean> {
    const extractionStatusSpinner = '//*[@data-test-id="extraction-status-icon-container"]/*[@data-icon="sync-alt"]';
    return elementExists(this.page, extractionStatusSpinner, { timeout });
  }

  /**
   * Open sidebar, wait for finished status, close sidebar.
   */
  async waitForCompletionAndClose(timeout?: number): Promise<boolean> {
    await this.open();
    const table = this.getHistoryTable();
    await table.waitUntilVisible();
    return Promise.all([
      this.page.waitForXPath(`${table.getXpath()}//*[@data-icon="check-circle" and @role="img"]`, {
        visible: true,
        timeout
      }),
      waitWhileLoading(this.page, { includeRuntimeSpinner: true, takeScreenshotOnFailure: false, timeout })
    ])
      .then(() => true)
      .catch(() => false)
      .finally(() => this.close());
  }
}
