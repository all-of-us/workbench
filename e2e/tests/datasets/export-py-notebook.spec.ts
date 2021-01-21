import DataResourceCard from 'app/component/data-resource-card';
import ExportToNotebookModal from 'app/component/export-to-notebook-modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {MenuOption, ResourceCard} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findOrCreateWorkspace, signInWithAccessToken} from 'utils/test-utils';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';

describe('Create Dataset', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

   /**
    * Create new Dataset, export to notebook in Python language
    * Finally delete Dataset.
    */
   test('Export dataset to notebook in Python language', async () => {
    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets(['Demographics']);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetBuildPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
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
    const analysisPage = await notebookPreviewPage.goAnalysisPage();

    // Verify new notebook exists.
    const resourceCard = new DataResourceCard(page);
    const notebookExists = await resourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(notebookExists).toBe(true);

    const origCardsCount = (await DataResourceCard.findAllCards(page)).length;

    // Delete notebook
    await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);

    // Resource cards count decrease by 1.
    const newCardsCount = (await DataResourceCard.findAllCards(page)).length;
    expect(newCardsCount).toBe(origCardsCount - 1);

    // Delete Dataset.
    await dataPage.openDatasetsSubtab();

    await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
  });

  /**
   * Test:
   * - Create dataset.
   * - Export dataset to notebook thru snowman menu.
   */
  test('Export dataset to notebook thru snowman menu', async () => {
    await findOrCreateWorkspace(page).then(card => card.clickWorkspaceName());

    // Click Add Datasets button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets(['Demographics']);
    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
    const datasetName = await saveModal.saveDataset({exportToNotebook: false});
    await waitWhileLoading(page);

    const resourceCard = new DataResourceCard(page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, {waitForNav: false});

    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    const notebookName = makeRandomName('test-notebook');
    await exportModal.fillInModal(notebookName);

    // Verify notebook created successfully. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    // Navigate to Workpace Notebooks page.
    const analysisPage = await notebookPreviewPage.goAnalysisPage();

    // Delete notebook
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);

    // Delete Dataset
    await analysisPage.openDatasetsSubtab();
    await analysisPage.deleteResource(datasetName, ResourceCard.Dataset);
  });

});
