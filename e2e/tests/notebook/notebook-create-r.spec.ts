import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { makeRandomName } from 'utils/str-utils';
import { ResourceCard, Tabs } from 'app/text-labels';
import expect from 'expect';
import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Create R kernel notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRKernelNotebookTest';
  const rNotebookName = makeRandomName('R');

  test('Duplicate rename delete notebook', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    // Start clone notebook.
    const cloneNotebookName = `Duplicate of ${rNotebookName}`;
    await analysisPage.duplicateNotebookViaTable(rNotebookName);

    // Rename notebook clone.
    const newNotebookName = makeRandomName('r-cloneNotebook');
    const modalTextContents = await analysisPage.renameResourceFromTable(
      cloneNotebookName,
      newNotebookName,
      ResourceCard.Notebook
    );
    expect(modalTextContents).toContain(`Enter new name for ${cloneNotebookName}.ipynb`);

    // Notebook card with new name is found.
    const dataResourceCard = new DataResourceCard(page);
    let notebookExist = await dataResourceCard.resourceExistsInTable(newNotebookName);
    expect(notebookExist).toBe(true);

    // Notebook card with old name is not found.
    notebookExist = await dataResourceCard.resourceExistsInTable(cloneNotebookName);
    expect(notebookExist).toBe(false);

    // Delete newly renamed notebook.
    await analysisPage.deleteResourceFromTable(newNotebookName, ResourceCard.Notebook);
    // Verify delete was successful.
    notebookExist = await dataResourceCard.resourceExistsInTable(newNotebookName);
    expect(notebookExist).toBe(false);

    // Delete R notebook
    await analysisPage.deleteResourceFromTable(rNotebookName, ResourceCard.Notebook);
    await analysisPage.waitForLoad();
  });
});
