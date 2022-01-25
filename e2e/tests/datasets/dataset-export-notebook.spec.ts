import WorkspaceDataPage from 'app/page/workspace-data-page';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Ethnicity, Sex } from 'app/page/cohort-participants-group';
import { Language, ConceptSetSelectValue, ResourceCard } from 'app/text-labels';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import DataResourceCard from 'app/component/data-resource-card';
import DatasetBuildPage from 'app/page/dataset-build-page';

// 30 minutes. Test involves starting of notebook that could take a long time to create.
jest.setTimeout(30 * 60 * 1000);

describe('Export Notebook Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const KernelLanguages = [{ LANGUAGE: Language.Python }, { LANGUAGE: Language.R }];
  const workspaceName = 'e2eDatasetToNotebookTest';
  const cohortName = makeRandomName('auotest', { includeHyphen: false });
  const datasetName = makeRandomName('auotest', { includeHyphen: false });

  /**
   * Test:
   * - Export dataset to a notebook. Run the notebook code and verify run results.
   * (Cohort and Dataset are saved and reused)
   */
  test.each(KernelLanguages)('Export to %s kernel notebook', async (kernelLanguage) => {
    await findOrCreateWorkspace(page, { workspaceName: workspaceName });
    await findOrCreateCohort(page, cohortName);
    const datasetBuildPage = await findOrCreateDataset(page, datasetName);

    const exportModal = await datasetBuildPage.clickAnalyzeButton();

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
    const dataPage = await notebookPage.goDataPage();

    // Delete Notebook.
    await dataPage.openAnalysisPage();
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });

  // Find an existing Cohort or create a new cohort to use in new dataset.
  async function findOrCreateCohort(page: Page, cohortName: string): Promise<void> {
    const dataPage = new WorkspaceDataPage(page);

    // Search for Cohort first. If found, return Cohort card.
    const existingCohortsCard = await dataPage.findCohortCard(cohortName);
    if (existingCohortsCard) {
      return;
    }

    // Create new.
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: Add Criteria: Ethnicity.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeEthnicity([Ethnicity.Skip]);
    // Include Participants Group 1: Add a second criteria.
    await group1.includeGenderIdentity([Sex.MALE]);

    // Save new cohort
    await cohortBuildPage.createCohort(cohortName);

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    await dataPage.openDataPage({ waitPageChange: true });
    await dataPage.waitForLoad();
  }

  // Find an existing dataset or create a new dataset.
  async function findOrCreateDataset(page: Page, datasetName: string): Promise<DatasetBuildPage> {
    let datasetBuildPage: DatasetBuildPage;

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const datasetCard = await new DataResourceCard(page).findCard(datasetName, ResourceCard.Dataset);
    if (datasetCard !== null) {
      await datasetCard.clickResourceName();
      datasetBuildPage = new DatasetBuildPage(page);
      await datasetBuildPage.waitForLoad();
      return datasetBuildPage;
    }

    datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Step 1: select user created cohort.
    await datasetBuildPage.selectCohorts([cohortName]);
    // Step 2: select demographics concept sets.
    await datasetBuildPage.selectConceptSets([ConceptSetSelectValue.Demographics]);

    const createModal = await datasetBuildPage.clickCreateButton();
    await createModal.createDataset();
    await datasetBuildPage.waitForLoad();
    return datasetBuildPage;
  }
});
