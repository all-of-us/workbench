import DataResourceCard from 'app/component/data-resource-card';
import ExportToNotebookModal from 'app/modal/export-to-notebook-modal';
import { Language, LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateDataset, findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';

// 10 minutes.
jest.setTimeout(10 * 60 * 1000);

describe('Dataset card snowman menu', () => {
  const KernelLanguages = [{ LANGUAGE: Language.R }, { LANGUAGE: Language.Python }];

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eDatasetSnowmanMenuExportToNotebookTest';

  /**
   * Test:
   * - Find or create a dataset.
   * - Export dataset to notebook via snowman menu.
   * - Notebook runtime is not started.
   */
  test.each(KernelLanguages)('Export to %s Jupyter notebook', async (kernelLanguage) => {
    await findOrCreateWorkspace(page, { workspaceName });

    const datasetName = await findOrCreateDataset(page, { openEditPage: false });

    // Find Dataset card.
    const datasetCard = await new DataResourceCard(page).findCard(datasetName, ResourceCard.Dataset);
    await datasetCard.selectSnowmanMenu(MenuOption.ExportToNotebook, { waitForNav: false });

    const exportModal = new ExportToNotebookModal(page);
    await exportModal.waitForLoad();

    // Check modal Export button is disabled.
    expect(await exportModal.waitForButton(LinkText.Export).isCursorNotAllowed()).toBe(true);

    const notebookName = makeRandomName('pyNotebook');
    const notebookPreviewPage = await exportModal.fillInModal(notebookName, kernelLanguage.LANGUAGE);

    // Verify notebook created successfully. Not going to start notebook runtime.
    const currentPageUrl = page.url();
    expect(currentPageUrl).toContain(`notebooks/preview/${notebookName}.ipynb`);

    // Delete notebook.
    const analysisPage = await notebookPreviewPage.goAnalysisPage();
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  });
});
