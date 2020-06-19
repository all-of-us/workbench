import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Link from 'app/element/link';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import DatasetSaveModal from 'app/page/dataset-save-modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import AnalysisPage from 'app/page/analysis-page';
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
    const newDatasetName = await saveModal.saveDataset({isExportToNotebook: true, notebookName: newNotebookName});
    await waitWhileLoading(page);

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = await page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const previewTextVisible = await waitForText(page, 'Preview (Read-Only)', {xpath: '//*[text()="Preview (Read-Only)"]'});
    expect(previewTextVisible).toBe(true);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('import pandas');
    expect(code).toContain('import os');

    // Navigate to Workpace Notebooks page.
    const notebooksLink = await Link.findByName(page, {name: 'Notebooks'});
    await notebooksLink.clickAndWait();

    const analysiPage = new AnalysisPage(page);
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
    await dataPage.openTab(TabLabelAlias.Datasets);
    await dataPage.deleteDataset(newDatasetName);

  });


});
