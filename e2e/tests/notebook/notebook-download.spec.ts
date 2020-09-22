import NotebookDownloadModal from 'app/page/notebook-download-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';

describe('Jupyter notebook download test', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  const testDownloadModal = async (modal: NotebookDownloadModal): Promise<void> => {
    let downloadBtn = await modal.getDownloadButton();
    expect(await downloadBtn.getProperty('disabled')).toBeTruthy();

    await modal.clickPolicyCheckbox();

    downloadBtn = await modal.getDownloadButton();
    const btnProps = await downloadBtn.getProperties();
    expect(btnProps['disabled']).toBeFalsy();
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
   * - for ipynb, pdf:
   *   - Download notebook.
   *   - Verify policy warnings modal interactions.
   */
  test('download notebook with policy warnings', async () => {
    // Viewport necessary for headless dialog positioning.
    await page.setViewport({height: 1280, width: 1280});

    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run some Python code so the notebook has content.
    await notebook.runCodeCell(1, {code: 'print("download test!")'});

    // Save and download.
    await notebook.save();

    // Event listener for new pages. Jupyter downloads yield new tabs for
    // each file download.
    const newTabUrls = [];
    browser.on('targetcreated', async (t) => {
      if (t.type() === 'page') {
        const p = await t.page();
        newTabUrls.push(p.url());
      }
    });

    console.log('downloading as ipynb');
    await testDownloadModal(await notebook.downloadAsIpynb());

    console.log('downloading as pdf');
    await testDownloadModal(await notebook.downloadAsPdf());

    // Wait a bit to process new tab creation events.
    await page.waitFor(500);

    // ipynb is special, and doesn't yield a recognizable URL for download.
    // As of 9/22/20 I was unable to find a clear mechanism for validating ipynb
    // download. All other formats, however, go through nbconvert - resulting
    // in a recognizable URL.
    expect(newTabUrls.find(url => {
      return url.includes('nbconvert/pdf') && url.includes(notebookName);
    })).toBeTruthy();
  }, 20 * 60 * 1000);

})
