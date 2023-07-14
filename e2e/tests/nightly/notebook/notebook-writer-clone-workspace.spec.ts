import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import { Language, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import { config } from 'resources/workbench-config';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('WRITER clone workspace and notebook tests', () => {
  const notebookName = makeRandomName('notebookWriterTest-Py3');
  const workspaceName = makeWorkspaceName();

  test('Create notebook and share workspace to WRITER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName });

    // Share workspace to a WRITER before creating new notebook.
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    await aboutPage.ensureCollaboratorAccess(config.WRITER_USER, WorkspaceAccessLevel.Writer);

    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);
    const notebookPage = await analysisPage.createNotebook(notebookName, Language.Python);

    // Run Python code.
    expect(
      await notebookPage.runCodeCell(1, {
        codeFile: 'resources/python-code/git-ignore-check.py',
        markdownWorkaround: true
      })
    ).toMatch(/success$/);

    await notebookPage.save();
  });
});
