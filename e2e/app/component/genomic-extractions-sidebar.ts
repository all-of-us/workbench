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
    await this.page.waitForTimeout(100);
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
   * Open sidebar, wait for finished status, close sidebar.
   */
  async waitForComplete(timeout?: number): Promise<boolean> {
    await this.open();
    try {
      const table = this.getHistoryTable();
      await table.waitUntilVisible();
      await this.page.waitForXPath(`${table.getXpath()}//*[@data-icon="check-circle" and @role="img"]`, {
        visible: true,
        timeout
      });
      return true;
    } catch (err) {
      return false;
    } finally {
      await this.close();
    }
  }
}
