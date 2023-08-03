import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { waitForFn } from 'utils/waits-utils';
import DataResourceCard from 'app/component/card/data-resource-card';
import NotebookDownloadModal from 'app/modal/notebook-download-modal';
import { getPropValue } from 'utils/element-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import expect from 'expect';
import { JupyterKernels } from 'app/text-labels';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe.skip('Python Kernel Notebook Test', () => {
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

  const workspaceName = 'e2eCreatePythonKernelNotebookTest';
  const pyNotebookName = makeRandomName('Py3');

  test('Run Python code and download notebook', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(pyNotebookName);

    // Verify kernel name.
    const kernelName = await notebook.getKernelName();
    expect(kernelName).toContain(JupyterKernels.Python);

    let cellIndex = 1;
    const cell1OutputText = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/python-code/import-os.py'
    });
    cellIndex++;
    // toContain() is not a strong enough check: error text also includes "success" because it's in the code
    expect(cell1OutputText).toMatch(/success$/);

    expect(
      await notebook.runCodeCell(cellIndex, {
        codeFile: 'resources/python-code/import-libs.py'
      })
    ).toMatch(/success$/);
    cellIndex++;

    await notebook.runCodeCell(cellIndex, { codeFile: 'resources/python-code/simple-pyplot.py' });

    // Verify plot is the output.
    const cell = notebook.findCell(cellIndex);
    const cellOutputElement = await cell.findOutputElementHandle();
    const [imgElement] = await cellOutputElement.$x('./img[@src]');
    expect(imgElement).toBeTruthy(); // plot format is a img.

    cellIndex++;

    // Save, exit notebook then come back from Analysis page.
    await notebook.save();
    await notebook.goAnalysisPage();

    // Find notebook card.
    const resourceCard = new DataResourceCard(page);
    const notebookCard = await resourceCard.findNameCellLinkFromTable({ name: pyNotebookName });
    await notebookCard.click();

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
  });
});
