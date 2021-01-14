import NotebookDownloadModal from 'app/page/notebook-download-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import {findOrCreateWorkspace, signInWithAccessToken} from 'utils/test-utils';
import {getPropValue} from 'utils/element-utils';
import {waitForFn} from 'utils/waits-utils';

describe('Jupyter notebook download test', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const testDownloadModal = async (modal: NotebookDownloadModal): Promise<void> => {
    const checkDownloadDisabledState = async (wantDisabled: boolean) => {
      expect(await waitForFn(async () => {
        const downloadBtn = await modal.getDownloadButton();
        return wantDisabled === !!(await getPropValue<boolean>(downloadBtn, 'disabled'));
      })).toBe(true);
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
  }

  /**
   * Test:
   * - Find an existing workspace.
   * - Create a new Notebook and add code.
   * - Save notebook.
   * - for ipynb, markdown:
   *   - Download notebook.
   *   - Verify policy warnings modal interactions.
   */
  test('download notebook with policy warnings', async () => {

    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run some Python code so the notebook has content.
    await notebook.runCodeCell(1, {code: 'print("download test!")'});

    // Save and download.
    await notebook.save();

    console.log('downloading as ipynb');
    await testDownloadModal(await notebook.downloadAsIpynb());

    console.log('downloading as Markdown');
    await testDownloadModal(await notebook.downloadAsMarkdown());

    // Ideally we would validate the download URLs or download content here.
    // As of 9/25/20 I was unable to find a clear mechanism for accessing this.

    await notebook.deleteNotebook(notebookName);
  }, 30 * 60 * 1000);

})
