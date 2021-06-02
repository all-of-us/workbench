import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { ResourceCard } from 'app/text-labels';
import { waitForFn } from 'utils/waits-utils';
import DataResourceCard from 'app/component/data-resource-card';
import NotebookDownloadModal from 'app/modal/notebook-download-modal';
import { getPropValue } from 'utils/element-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Create python kernel notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const testDownloadModal = async (modal: NotebookDownloadModal): Promise<void> => {
    const checkDownloadDisabledState = async (wantDisabled: boolean) => {
      expect(
        await waitForFn(async () => {
          const downloadBtn = await modal.getDownloadButton();
          return wantDisabled === !!(await getPropValue<boolean>(downloadBtn, 'disabled'));
        })
      ).toBe(true);
    };

    await checkDownloadDisabledState(true);
    await modal.clickPolicyCheckbox();

    await checkDownloadDisabledState(false);
    await modal.clickDownloadButton();

    // Clicking download opens the notebook download in a new tab. Bring the
    // main tab back to the front, otherwise the test just stalls out.
    await page.bringToFront();

    // Ideally we'd verify that the file was downloaded. Unfortunately this
    // seems to be non-trivial in Puppeteer: https://github.com/puppeteer/puppeteer/issues/299
    await modal.waitUntilClose();
  };

  const workspace = 'e2eCreatePythonKernelNotebookTest';

  test('Run Python code and download notebook', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const pyNotebookName = makeRandomName('Python3');
    const notebook = await dataPage.createNotebook(pyNotebookName);

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
    const notebookCard = await resourceCard.findCard(pyNotebookName, ResourceCard.Notebook);
    await notebookCard.clickResourceName();

    // Open notebook in Edit mode
    const notebookPreviewPage = new NotebookPreviewPage(page);
    await notebookPreviewPage.waitForLoad();
    await notebookPreviewPage.openEditMode(pyNotebookName);

    // Verify Code cell [1] output.
    const [, newCellOutput] = await notebook.getCellInputOutput(1);
    expect(newCellOutput).toEqual(cell1OutputText);

    // Save and download.
    await notebook.save();

    console.log('downloading as ipynb');
    await testDownloadModal(await notebook.downloadAsIpynb());

    console.log('downloading as Markdown');
    await testDownloadModal(await notebook.downloadAsMarkdown());

    // Ideally we would validate the download URLs or download content here.
    // As of 9/25/20 I was unable to find a clear mechanism for accessing this.

    await notebook.deleteNotebook(pyNotebookName);
  });
});
