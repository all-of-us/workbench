import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import DataResourceCard from 'app/component/data-resource-card';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { ResourceCard } from 'app/text-labels';

describe('Jupyter notebook tests in Python language', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Find an existing workspace.
   * - Create a new Notebook in Python language.
   * - Run python from .py files.
   * - Save notebook. Exit notebook.
   * - Reopen notebook in Edit mode.
   * - Verify contents are unchanged.
   */
  test(
    'Run code from file',
    async () => {
      await findOrCreateWorkspace(page);

      const dataPage = new WorkspaceDataPage(page);
      const notebookName = makeRandomName('py');
      const notebook = await dataPage.createNotebook(notebookName);

      // Verify kernel name.
      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('Python 3');

      const cell1OutputText = await notebook.runCodeCell(1, { codeFile: 'resources/python-code/import-os.py' });
      // toContain() is not a strong enough check: error text also includes "success" because it's in the code
      expect(cell1OutputText.endsWith('success')).toBeTruthy();

      const cell2OutputText = await notebook.runCodeCell(2, { codeFile: 'resources/python-code/import-libs.py' });
      // toContain() is not a strong enough check: error text also includes "success" because it's in the code
      expect(cell2OutputText.endsWith('success')).toBeTruthy();

      await notebook.runCodeCell(3, { codeFile: 'resources/python-code/simple-pyplot.py' });

      // Verify plot is the output.
      const cell = notebook.findCell(3);
      const cellOutputElement = await cell.findOutputElementHandle();
      const [imgElement] = await cellOutputElement.$x('./img[@src]');
      expect(imgElement).toBeTruthy(); // plot format is a img.

      const codeSnippet = '!jupyter kernelspec list';
      const codeSnippetOutput = await notebook.runCodeCell(4, { code: codeSnippet });
      expect(codeSnippetOutput).toEqual(expect.stringContaining('/usr/local/share/jupyter/kernels/python3'));

      // Save, exit notebook then come back from Analysis page.
      await notebook.save();
      await notebook.goAnalysisPage();

      // Find notebook card.
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, ResourceCard.Notebook);
      await notebookCard.clickResourceName();

      // Open notebook in Edit mode
      const notebookPreviewPage = new NotebookPreviewPage(page);
      await notebookPreviewPage.waitForLoad();
      await notebookPreviewPage.openEditMode(notebookName);

      // Verify Code cell [1] output.
      const [, newCellOutput] = await notebook.getCellInputOutput(1);
      expect(newCellOutput).toEqual(cell1OutputText);

      await notebook.deleteNotebook(notebookName);
    },
    30 * 60 * 1000
  );
});
