import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import DataPage from 'app/page/data-page';
import DatasetBuildPage from 'app/page/dataset-build-page';
import DatasetSaveModal from 'app/page/dataset-save-modal';
import {makeRandomName} from 'utils/str-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

describe('Create Dataset', () => {

  beforeEach(async () => {
    await signIn(page);
  });

   /**
    * Create new Dataset, Export to Notebook with Python language
    * Then delete Dataset.
    */
  test('Export dataset to notebook in Python programming language', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

      // Click Add Datasets button
    const dataPage = new DataPage(page);
    const addDatasetButton = await dataPage.getAddDatasetButton();
    await addDatasetButton.clickAndWait();

      // Build new Dataset
    const datasetPage = new DatasetBuildPage(page);
    await datasetPage.waitForLoad();

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.selectConceptSets(['Demographics']);

      // Preview table
    const previewTable = await datasetPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    await datasetPage.clickSaveAndAnalyzeButton();

    const saveModal = new DatasetSaveModal(page);
    const newNotebookName = makeRandomName();
    await saveModal.saveDataset({isExportToNotebook: true, notebookName: newNotebookName});
    await waitWhileLoading(page);

      // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = await page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('import pandas');
    expect(code).toContain('import os');

  });


});
