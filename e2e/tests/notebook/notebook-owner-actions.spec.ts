import DataResourceCard from 'app/component/data-resource-card';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import { LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';

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
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // All tests use same workspace and Python notebook.
  const workspaceName = 'e2eTestNotebookOwnerActions';
  let notebookName: string;

  test('Notebook name is unique', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.openAnalysisPage();
    await analysisPage.waitForLoad();

    // Find an existing notebook (Okay to use an existing notebook because we're not changing or editing this notebook).
    // Otherwise create a new notebook if no notebook is found.
    const aNotebookCard = await analysisPage.findNotebookCard();
    if (aNotebookCard) {
      notebookName = await aNotebookCard.getResourceName();
    } else {
      notebookName = makeRandomName('e2e');
      await analysisPage.createNotebook(notebookName).then((notebookPage) => notebookPage.goAnalysisPage());
    }

    // Attempt to create another notebook with same name. It should be blocked.
    await analysisPage.createNewNotebookLink().click();

    const modal = new NewNotebookModal(page);
    await modal.waitForLoad();
    await modal.name().type(notebookName);

    const errorTextXpath = `${modal.getXpath()}//*[text()="Name already exists"]`;
    const errorExists = await page.waitForXPath(errorTextXpath, { visible: true });
    expect(errorExists.asElement()).not.toBeNull();

    const disabledButton = await modal.createNotebookButton().isCursorNotAllowed();
    expect(disabledButton).toBe(true);

    // Click "Cancel" button to close modal.
    await modal.clickButton(LinkText.Cancel, { waitForClose: true });
    const modalExists = await modal.exists();
    expect(modalExists).toBe(false);

    // Page remain unchanged, still should be in Analysis page.
    expect(await analysisPage.isLoaded()).toBe(true);
  });

  test('Duplicate rename delete notebook', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    await dataPage.openAnalysisPage({ waitPageChange: true });
    const analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();

    // Before clone notebook, check to see if a notebook with cloneNotebookName is found. If yes, delete it now.
    const cloneNotebookName = `Duplicate of ${notebookName}`;

    const dataResourceCard = new DataResourceCard(page);
    let cardExists = await dataResourceCard.cardExists(cloneNotebookName, ResourceCard.Notebook);
    if (cardExists) {
      await analysisPage.deleteResource(cloneNotebookName, ResourceCard.Notebook);
    }

    // Start clone notebook.
    await analysisPage.duplicateNotebook(notebookName);

    // Rename notebook clone.
    const newNotebookName = makeRandomName('e2e-notebook');
    const modalTextContents = await analysisPage.renameResource(
      cloneNotebookName,
      newNotebookName,
      ResourceCard.Notebook
    );
    expect(modalTextContents).toContain(`Enter new name for ${cloneNotebookName}`);

    // Notebook card with new name is found.
    cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(true);

    // Notebook card with old name is not found.
    cardExists = await dataResourceCard.cardExists(cloneNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(false);

    // Delete newly renamed notebook.
    await analysisPage.deleteResource(newNotebookName, ResourceCard.Notebook);
    // Verify delete was successful.
    cardExists = await dataResourceCard.cardExists(newNotebookName, ResourceCard.Notebook);
    expect(cardExists).toBe(false);
  });
});
