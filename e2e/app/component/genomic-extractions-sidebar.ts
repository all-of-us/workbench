import BaseHelpSidebar from './base-help-sidebar';
import { SideBarLink } from 'app/text-labels';
import { logger } from 'libs/logger';
import { Page } from 'puppeteer';
import DataTable from './data-table';

export default class GenomicExtractionsSidebar extends BaseHelpSidebar {
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
    await this.page.waitForTimeout(1000);
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

  // Extraction status icon.
  async existStatusIcon(timeout?: number): Promise<boolean> {
    const extractionStatusSpinner = '//*[@data-test-id="extraction-status-icon-container"]/*[@data-icon="sync-alt"]';
    return this.page
      .waitForXPath(extractionStatusSpinner, { visible: true, timeout })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }

  /**
   * Open sidebar, check for status finished.
   */
  async waitForGenomicDataExtractionDone(timeout?: number): Promise<boolean> {
    const sidebar = new GenomicExtractionsSidebar(this.page);
    await sidebar.open();
    try {
      const table = sidebar.getHistoryTable();
      await table.waitUntilVisible();
      await this.page.waitForXPath(`${table.getXpath()}//*[@data-icon="check-circle" and @role="img"]`, {
        visible: true,
        timeout
      });
    } catch (err) {
      return false;
    } finally {
      await sidebar.close();
    }
  }
}
