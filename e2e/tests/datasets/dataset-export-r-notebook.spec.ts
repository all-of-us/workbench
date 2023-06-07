import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateDataset, findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { Language, ResourceCard, Tabs } from 'app/text-labels';
import { makeWorkspaceName } from 'utils/str-utils';
import { getPropValue } from 'utils/element-utils';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import DatasetBuildPage from 'app/page/dataset-build-page';
import { logger } from 'libs/logger';

describe('Export Dataset to Notebook Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = makeWorkspaceName();

  /**
   * Test:
   * - Export dataset to a notebook. Run the notebook code and verify run results.
   */
  test('Export to R kernel notebook in Build Dataset page', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspaceName });
    await findOrCreateDataset(page, { openEditPage: true });

    const buildPage = new DatasetBuildPage(page);
    await buildPage.waitForLoad();
    const exportModal = await buildPage.clickAnalyzeButton();

    const notebookName = makeRandomName();
    await exportModal.enterNotebookName(notebookName);
    await exportModal.pickLanguage(Language.R);

    let previewCodeLines = await exportModal.showCodePreview();
    expect(previewCodeLines.length).toBeGreaterThanOrEqual(1);
    // Verify few randomly selected code snippet
    expect(previewCodeLines.some((line) => line.includes('library(tidyverse)'))).toBe(true);
    expect(previewCodeLines.some((line) => line.includes('library(bigrquery)'))).toBe(true);
    logger.info({ previewCodeLines });

    await exportModal.clickExportButton();

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    previewCodeLines = await notebookPreviewPage.getFormattedCode();
    // Verify few randomly selected code snippet
    expect(previewCodeLines.some((line) => line.includes('Sys.getenv("OWNER_EMAIL")'))).toBe(true);
    expect(previewCodeLines.some((line) => line.includes('Sys.getenv("WORKSPACE_CDR")'))).toBe(true);
    expect(previewCodeLines.some((line) => line.includes('Sys.getenv("GOOGLE_PROJECT")'))).toBe(true);

    // Open notebook in Edit mode.
    const notebookPage = await notebookPreviewPage.openEditMode(notebookName);

    // Run all cells.
    await notebookPage.runAllCells();
    await notebookPage.waitForKernelIdle(2 * 60 * 1000, 5000);
    await notebookPage.save();

    // Last notebook cell contains a preview of the dataframe.
    const lastCell = await notebookPage.findLastCell();
    // Verify run output: Cell output format should be html table. Log error if failed.
    await lastCell.findRenderedHtmlElementHandle(2000).catch(() => lastCell.getOutputError());

    // Verify workspace name is in notebook page.
    const workspaceLink = await notebookPage.getWorkspaceLink().asElementHandle();
    expect(await getPropValue<string>(workspaceLink, 'textContent')).toEqual(workspaceName);

    // Verify notebook name is visible in notebook page.
    const notebookLinkXpath =
      `//a[contains(@href, "/${workspaceName.toLowerCase()}/notebooks/${notebookName.toLowerCase()}.ipynb")` +
      `and text()="${notebookName.toLowerCase()}"]`;
    const notebookLink = await page.waitForXPath(notebookLinkXpath, { visible: true });
    expect(notebookLink.asElement()).toBeTruthy();

    // Navigate to Workspace Data page.
    await notebookPage.goDataPage();

    // Delete Notebook.
    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);
    await analysisPage.deleteResourceFromTable(notebookName, ResourceCard.Notebook);
  });
});
