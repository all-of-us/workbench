import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName } from 'utils/str-utils';
import {
  findOrCreateCohort,
  findOrCreateDataset,
  findOrCreateWorkspace,
  openTab,
  signInWithAccessToken
} from 'utils/test-utils';
import { Language, ResourceCard, Tabs } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import DatasetBuildPage from 'app/page/dataset-build-page';

// 30 minutes. Test involves starting of notebook that could take a long time to create.
jest.setTimeout(30 * 60 * 1000);

const KernelLanguages = [{ LANGUAGE: Language.Python }, { LANGUAGE: Language.R }];
const workspaceName = 'e2eDatasetExportToNotebookTest';

describe('Export Notebook Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Export dataset to a notebook. Run the notebook code and verify run results.
   * (Cohort and Dataset are saved and reused)
   */
  test.each(KernelLanguages)('Export to %s Jupyter notebook', async (kernelLanguage) => {
    await findOrCreateWorkspace(page, { workspaceName: workspaceName });
    const cohortName = await findOrCreateCohort(page);
    await findOrCreateDataset(page, { cohortNames: [cohortName], openEditPage: true });

    const buildPage = new DatasetBuildPage(page);
    await buildPage.waitForLoad();
    const exportModal = await buildPage.clickAnalyzeButton();

    const notebookName = makeRandomName();
    await exportModal.enterNotebookName(notebookName);
    await exportModal.pickLanguage(kernelLanguage.LANGUAGE);
    await exportModal.showCodePreview();
    await exportModal.clickExportButton();

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    const code = await notebookPreviewPage.getFormattedCode();
    switch (kernelLanguage.LANGUAGE) {
      case Language.Python:
        expect(code.some((item) => item.includes('import pandas'))).toBe(true);
        expect(code.some((item) => item.includes('import os'))).toBe(true);
        break;
      case Language.R:
        expect(code.some((item) => item.includes('library(tidyverse)'))).toBe(true);
        expect(code.some((item) => item.includes('library(bigrquery)'))).toBe(true);
        break;
    }

    // Open notebook in Edit mode.
    const notebookPage = await notebookPreviewPage.openEditMode(notebookName);

    // Run all cells.
    await notebookPage.runAllCells();
    await notebookPage.waitForKernelIdle(2 * 60 * 1000, 5000);
    await notebookPage.save();

    // In both Python / R, the last cell contains a preview of the dataframe.
    const lastCell = await notebookPage.findLastCell();
    // Verify run output: Cell output format should be html table.
    await lastCell.findRenderedHtmlElementHandle();

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
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });
});
