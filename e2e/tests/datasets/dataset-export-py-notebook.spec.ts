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
  test('Export to Python kernel Jupyter notebook via dataset card snowman menu', async () => {
    await findOrCreateWorkspace(page, { workspaceName });
    const datasetName = await findOrCreateDataset(page, { openEditPage: false });

    const dataResourceCard = new DataResourceCard(page);
    // Find Dataset card. Select menu option "Export to Notebook"

    await dataResourceCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { name: datasetName, waitForNav: false });

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
    await analysisPage.deleteResourceFromTable(notebookName, ResourceCard.Notebook);
  });
});
