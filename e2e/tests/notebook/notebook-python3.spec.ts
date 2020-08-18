import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import NotebookPage from 'app/page/notebook-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {Language} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  describe('Notebooks in Python', () => {

    test('Save and open Python notebook', async () => {

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.openTab(TabLabelAlias.Analysis);

      const notebookName = makeRandomName('py-notebook');
      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.createNotebook(notebookName, Language.Python);

      const notebook = new NotebookPage(page, notebookName);
      await notebook.waitForLoad();

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('Python 3');

      await notebook.waitForKernelIdle();

      // Run few basic Python calls
      const codeSnippet1 =
           'import sys\n' +
           'print(sys.version)';
      const codeOutput1 = await notebook.runCodeCell(1, {code: codeSnippet1});
      // Python version is 3.7.6 at time of this writing. It can change.
      expect(codeOutput1).toEqual(expect.stringContaining('3.7.6'));

      const codeSnippet2 = '!jupyter kernelspec list';
      const codeOutput2 = await notebook.runCodeCell(2, {code: codeSnippet2});
      expect(codeOutput2).toEqual(expect.stringContaining('/usr/local/share/jupyter/kernels/python3'));

      await notebook.save();

      // Exit notebook but come back again from Analysis page.
      await notebook.goAnalysisPage();

      // Open saved notebook and verify notebook contents matches.
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
      await notebookCard.clickResourceName();

      const notebookPreviewPage = new NotebookPreviewPage(page);
      await notebookPreviewPage.waitForLoad();
      await notebookPreviewPage.openEditMode(notebookName);
      await notebook.waitForKernelIdle();

      // Get Code cell [1] input and output.
      const [newCode1, newOutput1] = await notebook.getCellInputOutput(1);

      // Get Code cell [2] input and output.
      const [newCode2, newOutput2] = await notebook.getCellInputOutput(2);

      // Delete Python notebook from Workspace Analysis page.
      await notebook.deleteNotebook(notebookName);

      // Verify Code cell [1] input and output.
      expect(newCode1).toEqual(codeSnippet1);
      expect(newOutput1).toEqual(codeOutput1);

      // Verify Code cell [2] input and output.
      expect(newCode2).toEqual(codeSnippet2);
      expect(newOutput2).toEqual(codeOutput2);
    })

  })

});
