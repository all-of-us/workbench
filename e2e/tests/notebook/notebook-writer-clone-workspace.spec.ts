import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { ResourceCard, Tabs } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe.skip('WRITER clone workspace and notebook tests', () => {
  const notebookName = makeRandomName('notebookWriterTest-Py3');
  const workspaceName = makeWorkspaceName();
  const writerWorkspaceName = 'e2eNotebookTestWriterWorkspace2';

  test('WRITER create workspace', async () => {
    await signInWithAccessToken(page, config.WRITER_USER);
    await findOrCreateWorkspace(page, { workspaceName: writerWorkspaceName });
  });

  test.skip('WRITER can clone workspace and edit notebook in workspace clone', async () => {
    // WRITER log in.
    await signInWithAccessToken(page, config.WRITER_USER);
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);

    await dataPage.cloneWorkspace();

    // Currently displayed workspace is the workspace clone.
    const workspaceCloneAnalysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, workspaceCloneAnalysisPage);

    // Create Notebook button is enabled.
    expect(await workspaceCloneAnalysisPage.createNewNotebookLink().isCursorNotAllowed()).toBe(false);

    const dataResourceCard = new DataResourceCard(page);
    const notebookCard = await dataResourceCard.findCard({
      name: notebookName,
      cardType: ResourceCard.Notebook,
      timeout: 60 * 1000
    }); // Need a longer wait time in case workspaces and notebook are new and created a short time ago.

    // Click notebook name.
    await notebookCard.clickName();
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();

    // Edit link is enabled.
    expect(await notebookPreviewPage.getEditLink().isCursorNotAllowed()).toBe(false);
    // Run (Playground mode) link is enabled.
    expect(await notebookPreviewPage.getRunPlaygroundModeLink().isCursorNotAllowed()).toBe(false);
    // Verify notebook code and answer are displayed.
    const previewCode = await notebookPreviewPage.getFormattedCode();
    expect(
      previewCode.some((item) => {
        return item.includes('success');
      })
    ).toBe(true);

    // Delete notebook and workspace clone.
    await notebookPreviewPage.goAnalysisPage();
    await dataPage.deleteResource(notebookName, ResourceCard.Notebook);
  });
});
