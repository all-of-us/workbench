import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import expect from 'expect';
import path from 'path';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Notebook Upload File Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eTestNotebookUploadFile';
  const pyNotebookName = makeRandomName('Py3');
  const pyFileName = 'nbstripoutput-filter.py';
  const pyFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${pyFileName}`);

  /*Skipping the test below as they will be moved to the new version of e2e test.
   * Story tracking this effort: https://precisionmedicineinitiative.atlassian.net/browse/RW-8763*/
  test.skip('Upload file and run Python code', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);

    const notebook = await dataPage.createNotebook(pyNotebookName);

    await notebook.uploadFile(pyFileName, pyFilePath);
    const codeOutput = await notebook.runCodeFile(1, pyFileName);
    expect(codeOutput).toMatch(/success$/);

    // Save, exit notebook page then delete notebook.
    await notebook.save();
    const analysisPage = await notebook.goAnalysisPage();
    await analysisPage.deleteResource(pyNotebookName, ResourceCard.Notebook);
    await analysisPage.waitForLoad();
  });
});
