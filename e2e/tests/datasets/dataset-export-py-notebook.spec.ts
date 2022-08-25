import DataResourceCard from 'app/component/card/data-resource-card';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import { Language, LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateDataset, findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';

describe('Export Dataset to Notebook Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eDatasetSnowmanMenuExportToPyNotebookTest';

  /**
   * Test:
   * - Find or create a dataset.
   * - Export dataset to notebook via snowman menu.
   * - Notebook runtime is not started.
   */
  /*Skipping the test below as they will be moved to the new version of e2e test.
   * Story tracking this effort: https://precisionmedicineinitiative.atlassian.net/browse/RW-8763*/
  test.skip('Export to Python kernel Jupyter notebook via dataset card snowman menu', async () => {
    await findOrCreateWorkspace(page, { workspaceName });
    const datasetName = await findOrCreateDataset(page, { openEditPage: false });

    // Find Dataset card. Select menu option "Export to Notebook"
    const datasetCard = await new DataResourceCard(page).findCard({
      name: datasetName,
      cardType: ResourceCard.Dataset
    });
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { waitForNav: false });

    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    // Check modal Export button is disabled.
    expect(await exportModal.waitForButton(LinkText.Export).isCursorNotAllowed()).toBe(true);

    const notebookName = makeRandomName('pyNotebook');
    const notebookPreviewPage = await exportModal.fillInModal(notebookName, Language.Python);

    // Verify notebook created successfully. Not going to start notebook runtime.
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    // Delete notebook.
    const analysisPage = await notebookPreviewPage.goAnalysisPage();
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });
});
