import DataResourceCard from 'app/component/data-resource-card';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { createWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';

describe('Export to notebook from dataset', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Create dataset.
   * - Export dataset to notebook thru snowman menu.
   */
  test('Create Jupyter notebook for Python programming language from existing dataset', async () => {
    await createWorkspace(page);

    // Click Add Datasets button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);
    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
    const datasetName = await saveModal.saveDataset({ exportToNotebook: false });
    await waitWhileLoading(page);

    const resourceCard = new DataResourceCard(page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { waitForNav: false });

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

    // Delete workspace
    await dataPage.deleteWorkspace();
  });
});
