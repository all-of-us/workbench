import DataResourceCard from 'app/component/data-resource-card';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import { withSignInTest } from 'libs/page-manager';

// 30 minutes. Starting a new notebook takes a while.
jest.setTimeout(30 * 60 * 1000);

/**
 * Notebook snowman actions tests:
 * - Duplicate
 * - Rename
 * - Delete
 * - Copy to another workspace is covered in nightly test "owner-copy-to-workspace.spec"
 */
describe('Workspace OWNER notebook snowman menu actions', () => {
  // All tests use same workspace and Python notebook.
  const workspace = 'e2eNotebookOwnerActionTest';
  const notebookName = makeRandomName('Py3');

  test('Notebook name must be unique in same workspace', async () => {
    await withSignInTest()(async (page) => {
      await findOrCreateWorkspace(page, { workspaceName: workspace });

      const dataPage = new WorkspaceDataPage(page);
      const notebookPage = await dataPage.createNotebook(notebookName);
      const analysisPage = await notebookPage.goAnalysisPage();

      // Attempt to create another notebook with same name. It should be blocked.
      await analysisPage.createNewNotebookLink().click();

      const modal = new NewNotebookModal(page);
      await modal.waitForLoad();
      await modal.name().type(notebookName);

      const errorTextXpath = `${modal.getXpath()}//*[text()="Name already exists"]`;
      const errorExists = await page.waitForXPath(errorTextXpath, { visible: true });
      expect(errorExists.asElement()).not.toBeNull();

      const createButton = modal.createNotebookButton();
      const disabledButton = await createButton.isCursorNotAllowed();
      expect(disabledButton).toBe(true);

      // Click "Cancel" button.
      await modal.clickButton(LinkText.Cancel, { waitForClose: true });
      const modalExists = await modal.exists();
      expect(modalExists).toBe(false);

      // Page remain unchanged, still should be in Analysis page.
      expect(await analysisPage.isLoaded()).toBe(true);
    });
  });

  test('Duplicate notebook', async () => {
    await withSignInTest()(async (page) => {
      await findOrCreateWorkspace(page, { workspaceName: workspace });

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();
      await dataPage.openAnalysisPage({ waitPageChange: true });
      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.waitForLoad();

      // Verify notebook exists.
      const dataResourceCard = new DataResourceCard(page);
      const notebookCard = await dataResourceCard.findCard(notebookName, ResourceCard.Notebook);
      expect(notebookCard).toBeTruthy();

      const cloneNotebookName = await analysisPage.duplicateNotebook(notebookName);
      await analysisPage.deleteResource(cloneNotebookName, ResourceCard.Notebook);
    });
  });

  test('Rename notebook', async () => {
    await withSignInTest()(async (page) => {
      await findOrCreateWorkspace(page, { workspaceName: workspace });

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();
      await dataPage.openAnalysisPage({ waitPageChange: true });
      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.waitForLoad();

      const newNotebookName = makeRandomName('test-notebook');
      const modalTextContents = await analysisPage.renameResource(notebookName, newNotebookName, ResourceCard.Notebook);
      expect(modalTextContents).toContain(`Enter new name for ${notebookName}`);

      // Notebook card with new name exists.
      const dataResourceCard = new DataResourceCard(page);
      let cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
      expect(cardExists).toBe(true);

      // Notebook card with old name exists.
      cardExists = await dataResourceCard.cardExists(notebookName, ResourceCard.Notebook);
      expect(cardExists).toBe(false);

      await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);
      cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
      expect(cardExists).toBe(false);
    });
  });
});
