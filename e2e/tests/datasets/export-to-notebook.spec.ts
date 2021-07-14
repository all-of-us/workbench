import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Ethnicity, Sex } from 'app/page/cohort-participants-group';
import { Language, LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import DataResourceCard from 'app/component/data-resource-card';
import { getPropValue } from 'utils/element-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import DeleteConfirmationModal from 'app/modal/delete-confirmation-modal';
import WarningDiscardChangesModal from 'app/modal/warning-discard-changes-modal';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import { TabLabels } from 'app/page/workspace-base';

// 30 minutes. Test involves starting of notebook that could take a long time to create.
jest.setTimeout(30 * 60 * 1000);

describe('Export dataset to notebook tests', () => {
  const KernelLanguages = [{ LANGUAGE: Language.Python }, { LANGUAGE: Language.R }];

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName({ includeHyphen: false });

  test('Export to Python kernel Jupyter notebook when editing dataset', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Dataset button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Create a new cohort to use in new dataset.
    await datasetBuildPage.clickAddCohortsButton();
    const cohortName = await createCohort();

    // Click Add Datasets button.
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    const datasetPage = await cohortActionsPage.clickCreateDatasetButton();

    // Step 1: select user created cohort.
    await datasetPage.selectCohorts([cohortName]);
    // Step 2: select demographics concept sets.
    await datasetPage.selectConceptSets([LinkText.Demographics]);

    // Export to Python language notebook.
    const notebookName = makeRandomName();
    const createModal = await datasetBuildPage.clickCreateButton();
    const datasetName = await createModal.createDataset();

    const exportModal = await datasetBuildPage.clickAnalyzeButton();

    await exportModal.enterNotebookName(notebookName);
    await exportModal.pickLanguage(Language.Python);
    await exportModal.clickExportButton();

    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    const analysisPage = await notebookPreviewPage.goAnalysisPage();

    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);

    // Navigate to Workspace Data page.
    await analysisPage.openDataPage({ waitPageChange: true });

    // Delete the cohort.
    await deleteCohort(cohortName, datasetName);

    // Associated dataset is gone after delete cohort.
    await dataPage.openTab(TabLabels.Datasets, { waitPageChange: false });
    expect(await new DataResourceCard(page).cardExists(datasetName, ResourceCard.Dataset)).toBe(false);
  });

  /**
   * Test:
   * - Create dataset and export to notebook. Start the notebook and run the dataset code.
   */
  // TODO Enable notebook test after bug fix. https://precisionmedicineinitiative.atlassian.net/browse/RW-6885
  xtest.each(KernelLanguages)('Export to %s kernel Jupyter notebook when creating dataset', async (kernelLanguage) => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Dataset button
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Create a new cohort to use in dataset.
    await datasetBuildPage.clickAddCohortsButton();
    const cohortName = await createCohort();

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    await cohortActionsPage.clickCreateDatasetButton();

    await datasetBuildPage.selectCohorts([cohortName]);
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetBuildPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    const createModal = await datasetBuildPage.clickCreateButton();
    const datasetName = await createModal.createDataset();

    await datasetBuildPage.clickAnalyzeButton();
    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    const notebookName = makeRandomName('notebook', { includeHyphen: false });
    await exportModal.enterNotebookName(notebookName);
    await exportModal.pickLanguage(kernelLanguage.LANGUAGE);
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
        expect(code.some((item) => item.includes('library(bigrquery)'))).toBe(true);
        break;
    }

    // Open notebook in Edit mode.
    const notebookPage = await notebookPreviewPage.openEditMode(notebookName);

    // Run notebook code cell #1.
    await notebookPage.runCodeCell(1);

    const cell1 = notebookPage.findCell(1);
    const cell1Output = await cell1.findOutputElementHandle();

    // Verify run output: Cell output format should be html table.
    const outputClassName = await getPropValue<string>(cell1Output, 'className');
    expect(outputClassName).toContain('rendered_html');

    // Verify workspace name is in notebook page.
    const workspaceLink = await notebookPage.getWorkspaceLink().asElementHandle();
    expect(await getPropValue<string>(workspaceLink, 'textContent')).toEqual(workspace);

    // Verify notebook name is visible in notebook page.
    const notebookLinkXpath =
      `//a[contains(@href, "/${workspace.toLowerCase()}/notebooks/${notebookName.toLowerCase()}.ipynb")` +
      `and text()="${notebookName.toLowerCase()}"]`;
    const notebookLink = await page.waitForXPath(notebookLinkXpath, { visible: true });
    expect(notebookLink.asElement()).toBeTruthy();

    // Navigate to Workspace Data page.
    await notebookPage.goDataPage();

    // Delete test data sequence is: Delete Notebook, then Dataset, finally Cohort.
    // Delete Notebook.
    await dataPage.openAnalysisPage();

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);

    // Delete Dataset.
    await dataPage.openDatasetsSubtab();

    const dataSetDeleteDialogText = await dataPage.deleteResource(datasetName, ResourceCard.Dataset);
    expect(dataSetDeleteDialogText).toContain(`Are you sure you want to delete Dataset: ${datasetName}?`);

    // Delete Cohort.
    await dataPage.openCohortsSubtab();

    const cohortDeleteDialogText = await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
    expect(cohortDeleteDialogText).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
  });

  // Create a new cohort to use in new dataset.
  async function createCohort(): Promise<string> {
    const cohortBuildPage = new CohortBuildPage(page);
    // Include Participants Group 1: Add Criteria: Ethnicity.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeEthnicity([Ethnicity.Skip]);
    // Include Participants Group 1: Add a second criteria.
    await group1.includeGenderIdentity([Sex.MALE]);
    return cohortBuildPage.createCohort();
  }

  // Delete cohort. Confirm warning about Dataset modal.
  async function deleteCohort(cohortName: string, _datasetName: string): Promise<void> {
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openCohortsSubtab();
    const dataResourceCard = new DataResourceCard(page);
    const cohortCard = await dataResourceCard.findCard(cohortName, ResourceCard.Cohort);
    await cohortCard.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });

    const deleteCohortModal = new DeleteConfirmationModal(page);
    await deleteCohortModal.waitForLoad();

    const modalTitle = await deleteCohortModal.getTitle();
    expect(modalTitle).toEqual(`Are you sure you want to delete Cohort: ${cohortName}?`);

    const modalText = await deleteCohortModal.getTextContent();
    console.log(modalText);
    expect(
      modalText.some((text) => {
        return text === 'This will permanently delete the Cohort and all associated review sets.';
      })
    ).toBeTruthy();

    await deleteCohortModal.clickButton(LinkText.DeleteCohort);

    const deleteWarningModal = new WarningDiscardChangesModal(page);
    await deleteWarningModal.waitForLoad();

    /*
    // Delete warning modal bug: https://precisionmedicineinitiative.atlassian.net/browse/RW-6759
    const warning = await deleteWarningModal.getTextContent();
    console.log(warning);
    expect(
      warning.some((item) => {
        item.includes(
          `The Cohort ${cohortName} is referenced by the following datasets: ${datasetName}. ` +
            `Deleting the Cohort ${cohortName} will make these datasets unavailable for use. ` +
            `Are you sure you want to delete ${cohortName} ?`
        );
      })
    ).toBe(true);
    */

    await deleteWarningModal.clickYesDeleteButton();
    // After delete cohort, dataset will disappear from UI and it is not accessible anymore.
  }
});
