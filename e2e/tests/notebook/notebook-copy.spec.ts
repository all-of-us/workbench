import {findWorkspace, signIn} from 'utils/test-utils';
import DataPage from 'app/page/data-page';
import {extractNamespace, makeRandomName} from 'utils/str-utils';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import Modal from 'app/component/modal';
import Link from 'app/element/link';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {LinkText} from 'app/text-labels';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook action tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Create new Workspace as the copy to destination Workspace.
   * - Create new Workspace as copy from Workspace and create new notebook in this Workspace.
   * - Run code to print WORKSPACE_NAMESPACE. It should match Workspace namespace in Workspace URL.
   * - Copy notebook to destination Workspace and give copied notebook a new name.
   * - Verify copied notebook is in destination Workspace.
   * - Open copied notebook and run code to print WORKSPACE_NAMESPACE. It should match destination Workspace namespace.
   * - Delete notebook.
   */
  test('Copy notebook to another Workspace', async () => {
    // Create destination workspace
    const toWorkspace = await findWorkspace(page, true).then(card => card.getWorkspaceName());

    // Create copy-from workspace
    await findWorkspace(page, true).then(card => card.clickWorkspaceName());

    // Create notebook in copy-from workspace.
    const copyFromNotebookName = makeRandomName('copy-from');
    const dataPage = new DataPage(page);

    // Get the billing project name from page url.
    let namespace = extractNamespace(new URL(page.url()));

    let notebookPage = await dataPage.createNotebook(copyFromNotebookName);

    // Run code to print out Workspace billing project name
    const code =
       'import os\n' +
       'print(os.getenv(\'WORKSPACE_NAMESPACE\'))';

    let codeOutput = await notebookPage.runCodeCell(1, {code});
    expect(codeOutput).toEqual(namespace);

    // Exit notebook and returns to the Workspace Analysis tab.
    let analysisPage = await notebookPage.goAnalysisPage();

    // Copy to destination Workspace and give notebook a new name.
    const copiedNotebookName = makeRandomName('copy-to');
    await analysisPage.copyNotebookToWorkspace(copyFromNotebookName, toWorkspace, copiedNotebookName);

    // Verify Copy Success modal.
    const modal = new Modal(page);
    await modal.waitForButton(LinkText.GoToCopiedNotebook);
    const textContent = await modal.getContent();
    expect(textContent).toContain(`Successfully copied ${copyFromNotebookName} to ${toWorkspace}`);
    // Dismiss modal.
    await modal.clickButton(LinkText.GoToCopiedNotebook, {waitForClose: true, waitForNav: true});

    // Verify current workspace is the destination Workspace
    analysisPage = new WorkspaceAnalysisPage(page);
    await analysisPage.waitForLoad();

    const workspaceLink = await Link.findByName(page, {name: toWorkspace});
    const linkDisplayed = await workspaceLink.isDisplayed();
    expect(linkDisplayed).toBe(true);

    // Get the destination Workspace billing project name.
    namespace = extractNamespace(new URL(page.url()));

    // Verify copy-to notebook exists in destination Workspace
    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findCard(copiedNotebookName, CardType.Notebook);
    expect(notebookCard).toBeTruthy();

    // Open copied notebook and run code to verify billing project name.
    await notebookCard.clickResourceName();
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    notebookPage = await notebookPreviewPage.openEditMode(copiedNotebookName);

    // Run same code and compare billing project name
    codeOutput = await notebookPage.runCodeCell(-1, {code});
    expect(codeOutput).toEqual(namespace);

    // Exit notebook. Returns to the Workspace Analysis tab.
    await notebookPage.goAnalysisPage();
    // Delete notebook
    await analysisPage.deleteNotebook(copiedNotebookName);

  })

});
