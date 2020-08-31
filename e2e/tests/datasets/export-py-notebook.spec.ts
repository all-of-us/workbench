import DataResourceCard from 'app/component/data-resource-card';
import ExportToNotebookModal from 'app/component/export-to-notebook-modal';
import Link from 'app/element/link';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {EllipsisMenuAction, ResourceCard, TabLabel} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';

describe('Create Dataset', () => {

  beforeEach(async () => {
    await signIn(page);
  });

   /**
    * Create new Dataset, export to notebook in Python language
    * Finally delete Dataset.
    */
  test('Export dataset to notebook in Python language', async () => {
    const workspaceCard = await findWorkspace(page);
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
    const notebooksLink = await Link.findByName(page, {name: 'Notebooks'});
    await notebooksLink.clickAndWait();

    const analysiPage = new WorkspaceAnalysisPage(page);
    await analysiPage.waitForLoad();

    // Verify new notebook exists.
    const resourceCard = new DataResourceCard(page);
    const notebookExists = await resourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(notebookExists).toBe(true);

    const origCardsCount = (await DataResourceCard.findAllCards(page)).length;

    // Delete notebook
    await analysiPage.deleteResource(newNotebookName, ResourceCard.Notebook);

    // Resource cards count decrease by 1.
    const newCardsCount = (await DataResourceCard.findAllCards(page)).length;
    expect(newCardsCount).toBe(origCardsCount - 1);

    // Delete Dataset.
    await dataPage.openTab(TabLabel.Data);
    await dataPage.openTab(TabLabel.Datasets, {waitPageChange: false});

    await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
  });

  /**
   * Test:
   * - Create dataset.
   * - Export dataset to notebook thru the ellipsis menu.
   */
  // TODO Eanble test after ticket fix. See https://precisionmedicineinitiative.atlassian.net/browse/RW-5388
  test.skip('Export dataset to notebook thru ellipsis menu', async () => {
    await findWorkspace(page).then(card => card.clickWorkspaceName());

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
    await datasetCard.clickEllipsisAction(EllipsisMenuAction.exportToNotebook, {waitForNav: false});

    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    const notebookName = makeRandomName('test-notebook');
    await exportModal.fillInModal(notebookName);

    // Verify notebook created successfully.
    await dataPage.openTab(TabLabel.Analysis);
    const cardExists = await resourceCard.cardExists(notebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(true);

    // Delete notebook.
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });

});
