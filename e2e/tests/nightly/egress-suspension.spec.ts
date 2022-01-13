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
  const egressFilename = 'generate-egress.py';
  const egressFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${egressFilename}`);

  test("VM egress suspends user's compute", async () => {
    await findOrCreateWorkspace(page);

    const dataPage = new WorkspaceDataPage(page);
    const notebookPage = await dataPage.createNotebook(notebookName);

    await notebookPage.uploadFile(egressFilename, egressFilePath);

    // Start the egress notebook and let it run in a tab. It may not complete
    // since our user may have their compute disabled while this is still
    // running. Intentionally do not wait for completion or check output.
    console.log('Generating egress via notebook');
    await notebookPage.startCodeFile(1, egressFilename, 5 * 60 * 1000);

    console.log('Awaiting security suspension in a new page');
    const newPage = await browser.newPage();
    await signInWithAccessToken(newPage, config.EGRESS_TEST_USER);
    // Open the current notebook URL again; this will instead show a suspended
    // error on reload once the user becomes suspended.
    await newPage.goto(page.url());

    await waitForSecuritySuspendedStatus(newPage);
  });
});
