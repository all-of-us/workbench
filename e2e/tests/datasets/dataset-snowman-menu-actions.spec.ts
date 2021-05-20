import DataResourceCard from 'app/component/data-resource-card';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Language, LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import DatasetRenameModal from 'app/modal/dataset-rename-modal';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import DatasetBuildPage from "app/page/dataset-build-page";

describe('Datasets card snowman menu actions', () => {
  const ProgrammingLanguage = [{ LANGUAGE: Language.R }, { LANGUAGE: Language.Python }];

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'aoutest-766371621540813'; //makeWorkspaceName();

  xtest('Rename dataset', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const datasetName = await createDataSet();

    // Find Dataset card.
    const dataBuildPage = await new DatasetBuildPage(page).clickDataTab();
    await dataBuildPage.openDatasetsSubtab();
    await waitWhileLoading(page);

    const resourceCard = new DataResourceCard(page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    expect(datasetCard).toBeTruthy();

    // Rename.
    await datasetCard.selectSnowmanMenu(MenuOption.RenameDataset, { waitForNav: false });

    const renameModal = new DatasetRenameModal(page);
    await renameModal.waitForLoad();

    const modalText = await renameModal.getTitle();
    expect(modalText).toEqual(`Enter new name for ${datasetName}`);

    // Type in same dataset name will not work.
    await renameModal.typeNewName(datasetName);
    await waitForText(page, 'New name already exists', { xpath: renameModal.getXpath() });
    expect(await renameModal.waitForButton(LinkText.RenameDataset).isCursorNotAllowed()).toBe(true);

    const newDatasetName = makeRandomName();
    await renameModal.typeNewName(newDatasetName);
    await renameModal.typeDescription('rename dataset test');
    await renameModal.clickButton(LinkText.RenameDataset, { waitForClose: true });

    // Verify existences of old and new Datasets cards.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openDatasetsSubtab();

    const newDatasetExists = await resourceCard.cardExists(newDatasetName, ResourceCard.Dataset);
    expect(newDatasetExists).toBe(true);

    const oldDatasetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(oldDatasetExists).toBe(false);

    await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
  });

  /**
   * Test:
   * - Create dataset.
   * - Export dataset to notebook thru snowman menu (the notebook is not created).
   */
  test.each(ProgrammingLanguage)('Export to %s kernel Jupyter notebook', async (each) => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const datasetName = await createDataSet();

    // Find Dataset card.
    const dataBuildPage = await new DatasetBuildPage(page).clickDataTab();
    await dataBuildPage.openDatasetsSubtab();
    await waitWhileLoading(page);

    const resourceCard = new DataResourceCard(page);
    const datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { waitForNav: false });

    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    // Check modal title and state of EXPORT AND OPEN button.
    const modalText = await exportModal.getTextContent();
    expect(modalText.some((text) => text.includes(`Export ${datasetName} to Notebook`))).toBe(true);
    expect(await exportModal.waitForButton(LinkText.Export).isCursorNotAllowed()).toBe(true);

    const notebookName = makeRandomName('pyNotebook');
    await exportModal.fillInModal(notebookName, each.LANGUAGE);

    // Verify notebook created successfully. Not going to start the Jupyter notebook.
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    // Navigate to Workspace Analysis page.
    const analysisPage = await notebookPreviewPage.goAnalysisPage();

    // Delete notebook first.
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);

    // Delete Dataset.
    await analysisPage.openDatasetsSubtab();
    await analysisPage.deleteResource(datasetName, ResourceCard.Dataset);
  });

  async function createDataSet(): Promise<string> {
    // Click Add Datasets button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetBuildPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    const createModal = await datasetBuildPage.clickCreateButton();
    const datasetName = await createModal.createDataset();
    await waitWhileLoading(page);
    return datasetName;
  }
});
