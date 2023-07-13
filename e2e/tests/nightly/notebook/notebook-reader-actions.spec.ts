import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Language, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Workspace READER Jupyter notebook action tests', () => {
  // Test reuse same workspace and notebook if exists.
  const workspaceName = makeWorkspaceName();
  const notebookName = makeRandomName('py', { includeHyphen: false });

  const pyCode = '!jupyter kernelspec list';
  const pyAnswer = 'python3';

  test('Share notebook to workspace READER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);

    // Share workspace to a READER before creating new notebook.
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(config.READER_USER, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);

    const notebook = await dataPage.createNotebook(notebookName, Language.Python);

    // Run Python code.
    const codeOutput = await notebook.runCodeCell(1, { code: pyCode });
    expect(codeOutput).toEqual(expect.stringContaining(pyAnswer));

    await notebook.save();

    const analysisPage = await notebook.goAnalysisPage();
    const notebookCard = await analysisPage.findNotebookCard(notebookName);
    expect(notebookCard).toBeTruthy();
  });
});
