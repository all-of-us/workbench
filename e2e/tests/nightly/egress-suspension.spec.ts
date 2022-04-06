import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import path from 'path';
import { waitForSecuritySuspendedStatus } from 'utils/runtime-utils';

// 45 minutes. Test could take a long time.
jest.setTimeout(45 * 60 * 1000);

describe('egress suspension', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page, config.EGRESS_TEST_USER);
  });

  const notebookName = makeRandomName('egress-notebook');
  const dataGenFilename = 'create-data-files.py';
  const dataGenFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${dataGenFilename}`);

  test("VM egress suspends user's compute", async () => {
    await findOrCreateWorkspace(page);

    const dataPage = new WorkspaceDataPage(page);
    const notebookPage = await dataPage.createNotebook(notebookName);

    await notebookPage.uploadFile(dataGenFilename, dataGenFilePath);

    // Generates 6 files currently. A single large file may time out.
    console.log('Generating 30MB files for download');
    await notebookPage.runCodeFile(1, dataGenFilename, 60 * 1000);

    // Download these 30MB files one-by-one from the file tree, generating ~180MB egress
    const treePage = await notebookPage.selectFileOpenMenu();
    // Allow some time for Jupyter extensions to load. Without this, download modal may fail to show.
    // TODO(RW-8114): Try waitForNetworkIdle here instead.
    await treePage.waitForTimeout(5000);
    for (let i = 0; i < 6; i++) {
      const f = `data${i}.txt`;
      console.log(`Downloading ${f} to generate egress`);
      await notebookPage.downloadFileFromTree(treePage, f);
    }

    console.log('Awaiting security suspension in a new page');
    const newPage = await browser.newPage();
    await signInWithAccessToken(newPage, config.EGRESS_TEST_USER);
    // Open the current notebook URL again; this will instead show a suspended
    // error on reload once the user becomes suspended.
    await newPage.goto(page.url());

    await waitForSecuritySuspendedStatus(newPage);
  });
});
