import DataResourceCard from 'app/component/data-resource-card';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';

describe('Create dataset and export to notebook at same time', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eDataSetsCreatePyNotebookTest';

  /**
   * Create new Dataset, export to notebook in Python language
   * Finally delete Dataset.
   */
  test('Jupyter Notebook for Python programming language can be created', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Datasets button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetBuildPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
    const newNotebookName = makeRandomName();
    const newDatasetName = await saveModal.saveDataset({ exportToNotebook: true, notebookName: newNotebookName });
    await waitWhileLoading(page);

    // Verify Notebook preview. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${newNotebookName}.ipynb`);

    const code = await notebookPreviewPage.getFormattedCode();
    expect(code).toContain('import pandas');
    expect(code).toContain('import os');

    // Navigate to Workspace Notebooks page.
    const analysisPage = await notebookPreviewPage.goAnalysisPage();

    // Verify new notebook exists.
    const resourceCard = new DataResourceCard(page);
    let notebookExists = await resourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(notebookExists).toBe(true);

    // Delete notebook
    await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);
    notebookExists = await resourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(notebookExists).toBe(false);

    // Delete Dataset.
    await dataPage.openDatasetsSubtab();
    await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
  });
});
