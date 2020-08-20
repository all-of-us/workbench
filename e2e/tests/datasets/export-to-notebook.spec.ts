import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Link from 'app/element/link';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import DatasetSaveModal from 'app/page/dataset-save-modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import {Ethnicity} from 'app/page/cohort-criteria-modal';
import {Language} from 'app/text-labels';

describe('Create Dataset', () => {

  beforeEach(async () => {
    await signIn(page);
  });

   /**
    * Create new Dataset, export to notebook in Python language
    * Finally delete Dataset.
    */
  test('Export dataset to notebook in Python programming language', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button.
    const dataPage = new DataPage(page);
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.selectConceptSets(['Demographics']);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    await datasetPage.clickSaveAndAnalyzeButton();

    const saveModal = new DatasetSaveModal(page);
    const newNotebookName = makeRandomName();
    const newDatasetName = await saveModal.saveDataset({exportToNotebook: true, notebookName: newNotebookName});
    await waitWhileLoading(page);

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const previewTextVisible = await waitForText(page, 'Preview (Read-Only)', {xpath: '//*[text()="Preview (Read-Only)"]'});
    expect(previewTextVisible).toBe(true);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('import pandas');
    expect(code).toContain('import os');

    // Navigate to Workpace Notebooks page.
    const notebooksLink = await Link.findByName(page, {name: 'Notebooks'});
    await notebooksLink.clickAndWait();

    const analysiPage = new WorkspaceAnalysisPage(page);
    await analysiPage.waitForLoad();

    // Verify new notebook exists.
    const resourceCard = new DataResourceCard(page);
    const notebookExists = await resourceCard.cardExists(newNotebookName, CardType.Notebook);
    expect(notebookExists).toBe(true);

    const origCardsCount = (await DataResourceCard.findAllCards(page)).length;

    // Delete notebook
    await analysiPage.deleteNotebook(newNotebookName);

    // Resource cards count decrease by 1.
    const newCardsCount = (await DataResourceCard.findAllCards(page)).length;
    expect(newCardsCount).toBe(origCardsCount - 1);

    // Delete Dataset.
    await dataPage.openTab(TabLabelAlias.Data);
    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});

    await dataPage.deleteDataset(newDatasetName);
  });

  /**
   * Create new Cohort thru Dataset Build page. Cohort built from demographics -> Ethnicity.
   * Create new Dataset with Cohort, then export to notebook in R language.
   * Delete Cohort, Dataset, and Notebook.
   */
  test('Export dataset to notebook in R programming language', async () => {
    const workspaceCard = await findWorkspace(page);
    const workspaceName = await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button
    const dataPage = new DataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();
    const cohortBuildPage = await datasetBuildPage.clickAddCohortsButton();

    // Include Participants Group 1: Add Criteria: Ethnicity
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const modal = await group1.includeEthnicity();
    await modal.addEthnicity([Ethnicity.HispanicOrLatino, Ethnicity.NotHispanicOrLatino]);
    await modal.clickFinishButton();

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Include Participants Group 1: ' + group1CountInt);

    // Save new Cohort.
    const newCohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${newCohortName}"`);

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.clickCreateDatasetButton();

    await datasetBuildPage.selectCohorts([newCohortName]);
    await datasetBuildPage.selectConceptSets(['Demographics']);
    await datasetBuildPage.clickSaveAndAnalyzeButton();

    const newNotebookName = makeRandomName();
    const saveModal = new DatasetSaveModal(page);
    const newDatasetName = await saveModal.saveDataset({
      exportToNotebook: true,
      notebookName: newNotebookName,
      lang: Language.R
    });
    await waitWhileLoading(page);

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('library(bigrquery)');

    // Navigate to Workpace Data page.
    const notebooksLink = await Link.findByName(page, {name: workspaceName});
    await notebooksLink.clickAndWait();
    await dataPage.waitForLoad();

    // Delete test data sequence is: Delete Notebook, then Dataset, finally Cohort.
    // Delete Notebook
    await dataPage.openTab(TabLabelAlias.Analysis);

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();
    await analysisPage.deleteNotebook(newNotebookName);

    // Delete Dataset
    await dataPage.openTab(TabLabelAlias.Data);
    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});

    const datasetDeleteDialogText = await dataPage.deleteDataset(newDatasetName);
    expect(datasetDeleteDialogText).toContain(`Are you sure you want to delete Dataset: ${newDatasetName}?`);

    // Delete Cohort
    await dataPage.openTab(TabLabelAlias.Cohorts, {waitPageChange: false});

    const cohortDeleteDialogText = await dataPage.deleteCohort(newCohortName);
    expect(cohortDeleteDialogText).toContain(`Are you sure you want to delete Cohort: ${newCohortName}?`);

  });


});
