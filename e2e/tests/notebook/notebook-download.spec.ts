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
    // XXX: In debug mode, test execution completely stops when a new tab is
    // opened for this download. Test can be resumed by closing the tab or
    // clicking back into the notebooks tab.
    await downloadBtn.click();

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
    // Event listener for taargetCreated events (new pages/popups)
    // adds the new page to the global.pages variable so it can be accessed immediately in the test that created it
    browser.on('targetcreated', async () => {
        console.log('New Tab Created');
        const pages = await browser.pages();
      console.log('global.pages.length', pages.length);
      console.log(pages.map(p => p.url()));
    });

    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('py');
    const notebook = await dataPage.createNotebook(notebookName);

    // Run some Python code so the notebook has content.
    await notebook.runCodeCell(1, {code: 'print("download test!")'});

    // Save and download.
    await notebook.save();

    await testDownloadModal(await notebook.downloadAsIpynb());
    console.log(page.url());
    await testDownloadModal(await notebook.downloadAsPdf());
  }, 20 * 60 * 1000);

})
