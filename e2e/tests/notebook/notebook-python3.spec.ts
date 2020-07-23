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

  describe('Create new notebooks', () => {

    test('Python 3 notebook', async () => {
      expect.assertions(8);

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.openTab(TabLabelAlias.Analysis);

      const notebookName = makeRandomName('test-notebook-');
      const analysisPage = new WorkspaceAnalysisPage(page);

      // Verify important messages in UI.
      const warningTexts = 'You are prohibited from taking screenshots or attempting in any way to remove participant-level data from the workbench.';
      const warningTextsXpath = `//*[contains(normalize-space(text()), "${warningTexts}")]`
      const initializeTexts = 'Initializing notebook server, may take up to 10 minutes';
      const initializeTextsXpath = `//*[@data-test-id="current-progress-card" and contains(normalize-space(), "${initializeTexts}")]`;
      const authenticateTexts = 'Authenticating with the notebook server';
      const authenticateTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${authenticateTexts}")]`;
      const creatingTexts = 'Creating the new notebook';
      const creatingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${creatingTexts}")]`
      const redirectingTexts = 'Redirecting to the notebook server';
      const redirectingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${redirectingTexts}")]`;

      await Promise.all([
        page.waitForXPath(warningTextsXpath, {visible: true}),
        page.waitForXPath(initializeTextsXpath, {visible: true}),
        page.waitForXPath(authenticateTextsXpath, {visible: true}),
        page.waitForXPath(creatingTextsXpath, {visible: true}),
        page.waitForXPath(redirectingTextsXpath, {visible: true}),
        analysisPage.createNotebook(notebookName, Language.Python),
      ]);

      const notebook = new NotebookPage(page, notebookName);
      await notebook.waitForLoad();

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('Python 3');

      // Always one empty code cell in an empty notebook
      const kernelState = await notebook.waitForKernelIdle();
      expect(kernelState).toBe(true);

      // Run few basic Python calls
      const code1 =
           'import sys\n' +
           'print(sys.version)';
      const code1Output = await notebook.runCodeCell(1, {code: code1});
      // Python version is 3.7.6 at time of this writing. It can change.
      expect(code1Output).toEqual(expect.stringContaining('3.7.6'));

      const code2 = '!jupyter kernelspec list';
      const code2Output = await notebook.runCodeCell(2, {code: code2});
      expect(code2Output).toEqual(expect.stringContaining('/usr/local/share/jupyter/kernels/python3'));

      await notebook.saveNotebook();

      // Exit notebook but come back again from Analysis page.
      await notebook.goBackAnalysisPage();

      // Open same notebook and verify notebook were saved successfully.
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
      await notebookCard.clickResourceName();

      const notebookPreviewPage = new NotebookPreviewPage(page);
      await notebookPreviewPage.waitForLoad();
      await notebookPreviewPage.openEditMode(notebookName);

      // Verify Code cell [1] input and output.
      const actualCode1 = await notebook.findCodeCellInput(1);
      expect(actualCode1).toEqual(code1);

      const actualOutput1 = await notebook.findCodeCellOutput(1);
      expect(actualOutput1).toEqual(code1Output);

      // Verify Code cell [2] input and output.
      const actualCode2 = await notebook.findCodeCellInput(2);
      expect(actualCode2).toEqual(code2);

      const actualOutput2 = await notebook.findCodeCellOutput(2);
      expect(actualOutput2).toEqual(code2Output);

      // Delete Python notebook from Workspace Analysis page.
      await notebook.goBackAnalysisPage();
      // ///await analysisPage.waitForLoad();

      // Find the notebook card and delete it.
      await dataPage.openTab(TabLabelAlias.Analysis);
      await resourceCard.findCard(notebookName, CardType.Notebook);
      await analysisPage.deleteNotebook(notebookName);
    })


  })

});
